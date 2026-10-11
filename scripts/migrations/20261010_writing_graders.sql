BEGIN;
ALTER TABLE writing_submissions ADD COLUMN IF NOT EXISTS assigned_grader_id bigint;
ALTER TABLE writing_submissions ADD COLUMN IF NOT EXISTS assigned_at timestamp(6) without time zone;
ALTER TABLE writing_submissions ADD COLUMN IF NOT EXISTS review_started_at timestamp(6) without time zone;
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conrelid='writing_submissions'::regclass AND contype='f'
          AND conkey=ARRAY[(SELECT attnum FROM pg_attribute WHERE attrelid='writing_submissions'::regclass AND attname='assigned_grader_id')]::smallint[]
    ) THEN
        ALTER TABLE writing_submissions ADD CONSTRAINT fk_writing_assigned_grader FOREIGN KEY (assigned_grader_id) REFERENCES users(id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid='writing_submissions'::regclass AND conname='ck_writing_assignment') THEN
        ALTER TABLE writing_submissions ADD CONSTRAINT ck_writing_assignment CHECK (
            status NOT IN ('ASSIGNED','IN_REVIEW') OR
            (grading_mode IS NOT DISTINCT FROM 'EXPERT' AND assigned_grader_id IS NOT NULL AND assigned_at IS NOT NULL)
        );
    END IF;
END $$;
CREATE INDEX IF NOT EXISTS ix_writing_dispatch_queue ON writing_submissions(submitted_at,id)
    WHERE grading_mode='EXPERT' AND status IN ('WAITING_REVIEW','ASSIGNED','IN_REVIEW');
CREATE INDEX IF NOT EXISTS ix_writing_grader_status ON writing_submissions(assigned_grader_id,status)
    WHERE grading_mode='EXPERT';
COMMIT;
