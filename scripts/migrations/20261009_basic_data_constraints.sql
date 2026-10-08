\set ON_ERROR_STOP on
BEGIN;
LOCK TABLE courses,user_course_enrollments,user_lesson_progress,user_flashcard_progress IN SHARE ROW EXCLUSIVE MODE;
-- Preserve the original erroneous, expired promotion before removing it.
CREATE TABLE IF NOT EXISTS course_pricing_repair_log (
 course_id bigint PRIMARY KEY, old_pricing jsonb NOT NULL, repaired_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
);
INSERT INTO course_pricing_repair_log(course_id,old_pricing)
 SELECT id,jsonb_build_object('price',price,'sale_price',sale_price,'sale_start_date',sale_start_date,'sale_end_date',sale_end_date)
 FROM courses WHERE sale_price IS NOT NULL AND (price IS NULL OR sale_price<0 OR sale_price>=price)
 AND sale_end_date < (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Ho_Chi_Minh') ON CONFLICT(course_id) DO NOTHING;
UPDATE courses SET sale_price=NULL,sale_start_date=NULL,sale_end_date=NULL
 WHERE sale_price IS NOT NULL AND (price IS NULL OR sale_price<0 OR sale_price>=price) AND sale_end_date<(CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Ho_Chi_Minh');
DO $$ BEGIN
 IF NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conname='uq_user_course_enrollments_pair' AND conrelid='user_course_enrollments'::regclass) THEN
  ALTER TABLE user_course_enrollments ADD CONSTRAINT uq_user_course_enrollments_pair UNIQUE(user_id,course_id);
 END IF;
 IF NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conname='uq_user_lesson_progress_pair' AND conrelid='user_lesson_progress'::regclass) THEN
  ALTER TABLE user_lesson_progress ADD CONSTRAINT uq_user_lesson_progress_pair UNIQUE(user_id,lesson_id);
 END IF;
 IF NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conname='uq_user_flashcard_progress_pair' AND conrelid='user_flashcard_progress'::regclass) THEN
  ALTER TABLE user_flashcard_progress ADD CONSTRAINT uq_user_flashcard_progress_pair UNIQUE(user_id,flashcard_id);
 END IF;
 IF NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conname='ck_course_pricing' AND conrelid='courses'::regclass) THEN
  ALTER TABLE courses ADD CONSTRAINT ck_course_pricing CHECK (
   (price IS NULL OR price>=0) AND
   (is_free IS DISTINCT FROM false OR (price IS NOT NULL AND price>0)) AND
   (sale_price IS NULL OR (price IS NOT NULL AND sale_price>=0 AND sale_price<price)) AND
   (sale_start_date IS NULL OR sale_end_date IS NULL OR sale_end_date>=sale_start_date) AND
   (access_duration_months IS NULL OR access_duration_months>=0)
  );
 END IF;
END $$;
COMMIT;
