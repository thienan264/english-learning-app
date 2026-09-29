import org.apache.poi.xwpf.usermodel.*;
import java.io.FileOutputStream;

public class GenerateTemplateWord {
    public static void main(String[] args) throws Exception {
        XWPFDocument document = new XWPFDocument();
        String text = """
[PASSAGE]
[TITLE] The Evolution of Urban Farming
[CONTENT]
In recent years, the concept of urban farming has evolved from a niche hobby to a critical component of sustainable city planning. As global populations continue to urbanize, the demand for fresh, locally sourced produce has skyrocketed. Traditional agriculture, while still the backbone of global food production, faces immense pressure from climate change, soil degradation, and the logistical nightmare of transporting goods across vast distances.

Vertical farming is one of the most promising developments in this sector. By utilizing stacked layers in controlled environments, these indoor farms can produce yields up to ten times higher per square meter than traditional outdoor farms. Furthermore, they use hydroponic systems that require 90% less water. A notable example is the "SkyGreens" facility in Singapore, which integrates rotating towers to ensure all plants receive adequate sunlight.

However, the initial capital required for such high-tech farming is substantial. LED lighting, climate control systems, and automated nutrient delivery mechanisms demand significant electrical power. Critics argue that unless this energy is derived from renewable sources, the carbon footprint of vertical farming might outweigh its benefits.

[GROUP]
[TYPE] MULTIPLE_CHOICE_SINGLE
[INSTRUCTION] Choose the correct letter, A, B, C or D.
[Q] 1. What was the original perception of urban farming?
[OPT] A. A major industry
[OPT] B. A niche hobby
[OPT] C. A government program
[OPT] D. A waste of time
[ANS] B
[Q] 2. What puts pressure on traditional agriculture?
[OPT] A. Too many farmers
[OPT] B. Climate change and soil degradation
[OPT] C. Lack of seeds
[OPT] D. Overabundance of water
[ANS] B
[Q] 3. How much less water do hydroponic systems use?
[OPT] A. 10%
[OPT] B. 50%
[OPT] C. 90%
[OPT] D. 100%
[ANS] C
[Q] 4. Where is SkyGreens located?
[OPT] A. Japan
[OPT] B. China
[OPT] C. Singapore
[OPT] D. USA
[ANS] C

[GROUP]
[TYPE] TRUE_FALSE_NOT_GIVEN
[INSTRUCTION] Do the following statements agree with the information given?
Write TRUE, FALSE, or NOT GIVEN.
[Q] 5. Urban farming is no longer considered important for city planning.
[ANS] FALSE
[Q] 6. Vertical farms can produce higher yields than outdoor farms.
[ANS] TRUE
[Q] 7. SkyGreens uses wind energy to power its rotating towers.
[ANS] NOT GIVEN
[Q] 8. The initial capital for high-tech farming is very low.
[ANS] FALSE

[GROUP]
[TYPE] FILL_IN_THE_BLANK
[INSTRUCTION] Complete the sentences below. Choose NO MORE THAN TWO WORDS from the passage.
[Q] 9. The demand for fresh, locally sourced produce has ______.
[ANS] skyrocketed
[Q] 10. Vertical farms utilize stacked layers in controlled ______.
[ANS] environments
[Q] 11. These farms use ______ systems instead of soil.
[ANS] hydroponic
[Q] 12. LED lighting and climate control demand significant electrical ______.
[ANS] power
[Q] 13. The carbon footprint might outweigh benefits unless energy comes from ______ sources.
[ANS] renewable

[PASSAGE]
[TITLE] The Psychology of Deep Space Travel
[CONTENT]
As humanity sets its sights on Mars and beyond, aerospace engineers are solving the physical challenges of interplanetary travel. However, psychologists are increasingly concerned about the mental toll such journeys will take on astronauts. A trip to Mars could take up to nine months each way, confining a small crew to a highly restricted environment.

Isolation and confinement (ICE) environments have been studied extensively on Earth, particularly in Antarctic research stations. Researchers have noted a phenomenon known as the "third-quarter syndrome," where morale plummets and interpersonal conflicts peak after the halfway mark of a mission, regardless of its total duration.

Communication delays add another layer of psychological stress. Depending on the planetary alignment, it can take up to 24 minutes for a message to travel from Mars to Earth. This lag means astronauts cannot rely on Mission Control for real-time problem-solving or emotional support during crises, forcing them to be completely autonomous.

To mitigate these issues, space agencies are developing virtual reality (VR) programs that simulate earthly environments. By wearing a headset, an astronaut can experience a walk through a forest or a beach sunset, providing crucial sensory variety that a sterile spacecraft lacks.

[GROUP]
[TYPE] MATCHING_INFORMATION
[INSTRUCTION] Which paragraph contains the following information? Write the correct letter A, B, C or D.
[Q] 14. An example of a technology designed to reduce sensory deprivation.
[ANS] D
[Q] 15. The exact maximum time a message takes to reach Earth.
[ANS] C
[Q] 16. The specific point in a mission when psychological issues often arise.
[ANS] B
[Q] 17. The estimated travel time to Mars.
[ANS] A
[Q] 18. The necessity for crew autonomy during emergencies.
[ANS] C
[Q] 19. A comparison with earthly research facilities.
[ANS] B

[GROUP]
[TYPE] MULTIPLE_CHOICE_MULTI
[INSTRUCTION] Choose TWO letters, A-E. Which TWO psychological challenges are mentioned in the text?
[Q] 20. Select two challenges
[OPT] A. Lack of oxygen
[OPT] B. Isolation and confinement
[OPT] C. Communication delays
[OPT] D. Unhealthy diet
[OPT] E. Muscle atrophy
[ANS] B, C

[GROUP]
[TYPE] SUMMARY_COMPLETION
[INSTRUCTION] Complete the summary below using the list of words.
[Q] 21. A journey to Mars will confine crews to a highly restricted ______.
[ANS] environment
[Q] 22. In Antarctic stations, researchers observe the "third-quarter ______".
[ANS] syndrome
[Q] 23. Communication delays can take up to 24 ______.
[ANS] minutes
[Q] 24. Astronauts will be completely ______ during crises.
[ANS] autonomous
[Q] 25. Space agencies are using virtual ______ to provide sensory variety.
[ANS] reality
[Q] 26. A sterile ______ lacks sensory stimulation.
[ANS] spacecraft

[PASSAGE]
[TITLE] Bioluminescence in Deep Sea Creatures
[CONTENT]
In the darkest depths of the ocean, where sunlight cannot penetrate, nature has engineered its own form of illumination: bioluminescence. This biochemical emission of light by living organisms is one of the most widespread phenomena in the marine environment, particularly below 200 meters.

The chemical reaction that produces bioluminescence involves a light-emitting molecule called luciferin and an enzyme called luciferase. When these mix in the presence of oxygen, they generate a cold light, meaning almost 100% of the energy is released as light rather than heat. This extreme efficiency is something human engineers have long envied and tried to replicate in artificial lighting.

Marine animals use bioluminescence for a variety of critical survival functions. The anglerfish, for example, dangles a glowing lure from its forehead to attract unsuspecting prey in the pitch black. Other species, like the deep-sea squid, use light as a defense mechanism. When threatened, instead of emitting a cloud of dark ink—which would be invisible in the dark—they release a blinding cloud of glowing chemicals to confuse predators.

Another fascinating application is counter-illumination. The hatchetfish has light-producing organs on its underbelly. By matching the faint light filtering down from the surface, it effectively erases its own silhouette, becoming invisible to predators swimming below.

[GROUP]
[TYPE] MATCHING_INFORMATION
[INSTRUCTION] Match the marine animal with its use of bioluminescence.
[Q] 27. Anglerfish
[ANS] Attracting prey
[Q] 28. Deep-sea squid
[ANS] Confusing predators
[Q] 29. Hatchetfish
[ANS] Hiding from predators below
[Q] 30. Firefly (not in text)
[ANS] NOT GIVEN
[Q] 31. Luciferin
[ANS] Molecule
[Q] 32. Luciferase
[ANS] Enzyme

[GROUP]
[TYPE] FILL_IN_THE_BLANK
[INSTRUCTION] Complete the sentences.
[Q] 33. Bioluminescence is widespread below ______ meters.
[ANS] 200
[Q] 34. The reaction requires the presence of ______.
[ANS] oxygen
[Q] 35. Almost 100% of the energy is released as ______.
[ANS] light
[Q] 36. The hatchetfish has light-producing organs on its ______.
[ANS] underbelly

[GROUP]
[TYPE] MULTIPLE_CHOICE_SINGLE
[INSTRUCTION] Choose the correct letter.
[Q] 37. What type of light is bioluminescence?
[OPT] A. Hot light
[OPT] B. Cold light
[OPT] C. UV light
[OPT] D. Infrared light
[ANS] B
[Q] 38. What does the anglerfish use to attract prey?
[OPT] A. A loud sound
[OPT] B. A glowing lure
[OPT] C. A chemical cloud
[OPT] D. Fast movement
[ANS] B
[Q] 39. Why doesn't the deep-sea squid use dark ink?
[OPT] A. It ran out of ink
[OPT] B. It is invisible in the dark
[OPT] C. Predators like ink
[OPT] D. It is too cold
[ANS] B
[Q] 40. What is counter-illumination used for?
[OPT] A. Erasing silhouette
[OPT] B. Communicating
[OPT] C. Finding a mate
[OPT] D. Seeing in the dark
[ANS] A
""";

        String[] lines = text.split("\\n");
        for (String line : lines) {
            XWPFParagraph p = document.createParagraph();
            XWPFRun r = p.createRun();
            if (line.trim().startsWith("[")) {
                r.setBold(true);
                r.setColor("FF0000"); // Make tags red for visibility
            }
            r.setText(line);
        }

        String outPath = "/Users/thienan/Desktop/IELTS_Strict_Template_Test.docx";
        try (FileOutputStream out = new FileOutputStream(outPath)) {
            document.write(out);
        }
        System.out.println("Created file: " + outPath);
    }
}
