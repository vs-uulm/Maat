package de.uulm.vs.autodetect.test;

import no.uio.subjective_logic.opinion.SubjectiveOpinion;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Class for testing some subjective opinion fusion stuff
 */
public class FusionTest {
    private final Logger l = LogManager.getLogger(getClass());

    private SubjectiveOpinion soVacuous;
    private SubjectiveOpinion soNeutral;
    private SubjectiveOpinion soP;
    private SubjectiveOpinion soPP;
    private SubjectiveOpinion soPD;
    private SubjectiveOpinion soN;
    private SubjectiveOpinion soNN;
    private SubjectiveOpinion soND;

    private final double ATOMICITY = 0.5; // fixed baserate
    private final double NEUTRAL = 0.0;
    private final double SLIGHTLY = 0.3;
    private final double VERY = 0.7;
    private final double DOGMATCIC = 1.0;

    private final double HALFLIFE = 10; // halflife time

    @Before
    public void setUp() {
        l.info("Setting up opinions...");
        soVacuous = new SubjectiveOpinion(0.0, 0.0, 1.0, ATOMICITY);
        soNeutral = new SubjectiveOpinion(SLIGHTLY, SLIGHTLY, 1 - 2 * SLIGHTLY, ATOMICITY);
        soP = new SubjectiveOpinion(SLIGHTLY, 0, 1 - SLIGHTLY, ATOMICITY);
        soPP = new SubjectiveOpinion(VERY, 0, 1 - VERY, ATOMICITY);
        soN = new SubjectiveOpinion(0, SLIGHTLY, 1 - SLIGHTLY, ATOMICITY);
        soNN = new SubjectiveOpinion(0, VERY, 1 - VERY, ATOMICITY);
        soPD = new SubjectiveOpinion(1, true);
        soND = new SubjectiveOpinion(0, true);
        l.debug(String.format("Vacuous opinion: %s", soVacuous.toString()));
        l.debug(String.format("Neutral opinion: %s", soNeutral.toString()));
        l.debug(String.format("Slightly positive opinion: %s", soP.toString()));
        l.debug(String.format("Very positive opinion: %s", soPP.toString()));
        l.debug(String.format("Slightly negative opinion: %s", soN.toString()));
        l.debug(String.format("Very negative opinion: %s", soNN.toString()));
        l.debug(String.format("Dogmatic positive opinion: %s", soPD.toString()));
        l.debug(String.format("Dogmatic negative opinion: %s", soND.toString()));
    }

    @Test
    public void testCCFusion_1() {
        l.info("Testing CC Fusion 1");

        SubjectiveOpinion test = new SubjectiveOpinion(soN);
        test = test.ccFuse(new SubjectiveOpinion(soP));
        test = test.ccFuse(new SubjectiveOpinion(soP));
        test = test.ccFuse(new SubjectiveOpinion(soNN));
        l.info(String.format("CC fused opinion: %s", test));
        Assert.assertTrue(test.isConsistent());
    }

    @Test
    public void testCCFusion_2() {
        l.info("Testing CC Fusion 2");

        List<SubjectiveOpinion> opinions = new ArrayList<>();
        opinions.add(new SubjectiveOpinion(soN));
        opinions.add(new SubjectiveOpinion(soP));
        opinions.add(new SubjectiveOpinion(soP));
        opinions.add(new SubjectiveOpinion(soNN));
        SubjectiveOpinion res = SubjectiveOpinion.ccFuse(opinions); // *NOT* the right way to fuse more than two opinions!
        l.info(String.format("CC fused opinion: %s", res));
        Assert.assertTrue(res.isConsistent());
    }

    @Test
    public void testCCFusion_3() {
        l.info("Testing CC Fusion 3");
        SubjectiveOpinion oNAN = SubjectiveOpinion.ccFuse(pastMisbehavior());
        l.info("NaN case? " + oNAN);
        Assert.assertTrue(oNAN.getBelief() != Double.NaN && oNAN.getDisbelief() != Double.NaN
                && oNAN.getUncertainty() != Double.NaN);
        Assert.assertTrue(oNAN.isConsistent());
    }

    @Test
    public void testCCFusion_Collection() {
        l.info("Testing CC Fusion collection and binary method");

        List<SubjectiveOpinion> opinions = new ArrayList<>();
        opinions.add(new SubjectiveOpinion(soN));
        opinions.add(new SubjectiveOpinion(soPP));
        SubjectiveOpinion resCol = SubjectiveOpinion.ccCollectionFusion(opinions);
        l.info(String.format("CC fused opinion collection method: %s", resCol));

        SubjectiveOpinion res = soN.ccFuse(soPP);
        l.info(String.format("CC fused opinion regular method: %s", res));

        Assert.assertTrue(res.equals(resCol));
    }

    //@Test
    public void testCCFusion_Associativity() {
        l.info("Testing CC Fusion 4 semi-associative property");

        List<SubjectiveOpinion> opinions = new ArrayList<>();
        opinions.add(new SubjectiveOpinion(soN));
        opinions.add(new SubjectiveOpinion(soP));
        opinions.add(new SubjectiveOpinion(soP));
        opinions.add(new SubjectiveOpinion(soNN));
        SubjectiveOpinion res = SubjectiveOpinion.ccFuse(opinions);
        l.info(String.format("CC fused opinion collection naive: %s", res));
        Assert.assertTrue(res.isConsistent());

        // use semi-associative fusion operator for more than 2 opinions
        SubjectiveOpinion resCol = SubjectiveOpinion.ccCollectionFusion(opinions);
        l.info(String.format("CC fused opinion collection semi-associative: %s", resCol));
        Assert.assertTrue(resCol.isConsistent());

        if (opinions.size() > 2) {
            Assert.assertFalse(resCol.equals(res));
        }

        // try with a different order
        List<SubjectiveOpinion> opinions2 = new ArrayList<>();
        opinions2.add(new SubjectiveOpinion(soP));
        opinions2.add(new SubjectiveOpinion(soN));
        opinions2.add(new SubjectiveOpinion(soNN));
        opinions2.add(new SubjectiveOpinion(soP));
        SubjectiveOpinion resCol2 = SubjectiveOpinion.ccCollectionFusion(opinions2);
        l.info(String.format("CC fused opinion collection semi-associative: %s", resCol2));
        Assert.assertTrue(resCol2.isConsistent());

        //TODO: this should not fail
        Assert.assertTrue("Different collection order produces different CC fusion result", resCol.equals(resCol2));
    }

    //@Test
    public void testCCFusion_Idempotence() {
        l.info("Testing CC Fusion idempotence property");
        List<SubjectiveOpinion> opinions = new ArrayList<>();
        opinions.add(new SubjectiveOpinion(soP));
        opinions.add(new SubjectiveOpinion(soP));

        SubjectiveOpinion res = soP.ccFuse(soP);
        SubjectiveOpinion resCol = SubjectiveOpinion.ccCollectionFusion(opinions);

        l.info(String.format("CC fused two times identical opinion %s to %s", soP, res));
        l.info(String.format("CC fused two times identical opinion %s to %s", soP, resCol));
        Assert.assertEquals(res, soP);
        Assert.assertEquals(resCol, soP);
    }

    @Test
    public void testCCFusion_Commutativity() {
        l.info("Testing CC Fusion commutative property");

        SubjectiveOpinion res1 = soP.ccFuse(soNN);
        SubjectiveOpinion res2 = soNN.ccFuse(soP);
        l.info(String.format("Commuative results: %s , %s", res1, res2));

        Assert.assertTrue(res1.equals(res2));
    }

    @Test
    public void testCCFusion_Neutrality() {
        l.info("Testing CC Fusion neutral element property");

        SubjectiveOpinion initialOpinion = new SubjectiveOpinion(soP);
        SubjectiveOpinion res1 = initialOpinion.ccFuse(SubjectiveOpinion.UNCERTAIN);

        List<SubjectiveOpinion> opinions = new ArrayList<>();
        opinions.add(new SubjectiveOpinion(initialOpinion));
        opinions.add(new SubjectiveOpinion(SubjectiveOpinion.UNCERTAIN));
        opinions.add(new SubjectiveOpinion(SubjectiveOpinion.UNCERTAIN));
        SubjectiveOpinion res2 = initialOpinion.ccCollectionFusion(opinions);

        Assert.assertTrue(res1.equals(initialOpinion));
        Assert.assertTrue(res2.equals(initialOpinion));
    }

    @Test
    public void testCCFusion_NaN(){
        SubjectiveOpinion test = new SubjectiveOpinion(soP);
        test.ccFuse(new SubjectiveOpinion(soP));
        Assert.assertTrue(test.isConsistent());
    }

    @Test
    public void testWBFusion_Idempotence() {
        l.info("Testing WB Fusion idempotence property");

        SubjectiveOpinion res = soP.wbFuse(soP);

        l.info(String.format("Fused two times identical opinion %s to %s", soP, res));
        Assert.assertTrue(res.equals(soP));

        // vacuous arguments
        Assert.assertTrue(soVacuous.wbFuse(soVacuous).equals(soVacuous));

        // dogmatic arguments
        SubjectiveOpinion resD = soPD.wbFuse(soPD);
        l.info(String.format("Fused two times identical opinion %s to %s", soPD, resD));
        Assert.assertTrue(resD.equals(soPD));
    }

    @Test
    public void testWBFusion_Commutativity() {
        l.info("Testing WB Fusion commutative property");

        // non-vacuous, non-dogmatic
        SubjectiveOpinion res1 = soP.wbFuse(soNN);
        SubjectiveOpinion res2 = soNN.wbFuse(soP);
        l.info(String.format("Commuative results: %s , %s", res1, res2));
        Assert.assertTrue(res1.equals(res2));

        // one vacuous argument
        Assert.assertTrue(soP.wbFuse(soVacuous).equals(soVacuous.wbFuse(soP)));

        // dogmatic arguments
        Assert.assertTrue(soPD.wbFuse(soND).equals(soND.wbFuse(soPD)));
    }

    @Test
    public void testWBFusion_Neutrality() {
        l.info("Testing WB Fusion neutral element property");

        SubjectiveOpinion initialOpinion = new SubjectiveOpinion(soP);
        SubjectiveOpinion res = initialOpinion.wbFuse(SubjectiveOpinion.UNCERTAIN);

        Assert.assertTrue(res.equals(initialOpinion));
    }

    @Test
    public void testCumulativeFusion() {
        l.info("Testing Cumulative Fusion");

        List<SubjectiveOpinion> opinions = pastMisbehavior();
        SubjectiveOpinion result = SubjectiveOpinion.cumulativeFuse(opinions);
        l.info(String.format("Cumulative fused opinion: %s", result.toString()));

        List<SubjectiveOpinion> opinions2 = new ArrayList<>();
        opinions2.add(new SubjectiveOpinion(soP));
        opinions2.add(new SubjectiveOpinion(soN));
        opinions2.add(new SubjectiveOpinion(soN));
        opinions2.add(new SubjectiveOpinion(soP));
        SubjectiveOpinion result2 = SubjectiveOpinion.cumulativeFuse(opinions2);
        l.info(String.format("Cumulative fused opinion: %s", result2.toString()));
    }

    @Test
    public void compareFusionOperators() {
        SubjectiveOpinion o1, o2;

        o1 = new SubjectiveOpinion(soP);
        o2 = new SubjectiveOpinion(soN);
        l.info("----- Slight belief and slight disbelief -----");
        l.info(String.format("Fusing opinions    %s and %s", o1.toString(), o2.toString()));
        l.info(String.format("Cumulative result: %s", o1.cumulativeFuse(o2).toString()));
        l.info(String.format("WBF result:        %s", o1.wbFuse(o2).toString()));
        l.info(String.format("CC result:         %s", o1.ccFuse(o2).toString()));


        o1 = new SubjectiveOpinion(soPP);
        o2 = new SubjectiveOpinion(soN);
        l.info("----- High belief and slight disbelief -----");
        l.info(String.format("Fusing opinions    %s and %s", o1.toString(), o2.toString()));
        l.info(String.format("Cumulative result: %s", o1.cumulativeFuse(o2).toString()));
        l.info(String.format("WBF result:        %s", o1.wbFuse(o2).toString()));
        l.info(String.format("CC result:         %s", o1.ccFuse(o2).toString()));

        l.info("----- High belief and slight belief -----");
        o1 = new SubjectiveOpinion(soPP);
        o2 = new SubjectiveOpinion(soP);
        l.info(String.format("Fusing opinions    %s and %s", o1.toString(), o2.toString()));
        l.info(String.format("Cumulative result: %s", o1.cumulativeFuse(o2).toString()));
        l.info(String.format("WBF result:        %s", o1.wbFuse(o2).toString()));
        l.info(String.format("CC result:         %s", o1.ccFuse(o2).toString()));

        o1 = new SubjectiveOpinion(soPD);
        o2 = new SubjectiveOpinion(soNN);
        l.info("----- Dogmatic belief and high disbelief -----");
        l.info(String.format("Fusing opinions    %s and %s", o1.toString(), o2.toString()));
        l.info(String.format("Cumulative result: %s", o1.cumulativeFuse(o2).toString()));
        l.info(String.format("WBF result:        %s", o1.wbFuse(o2).toString()));
        l.info(String.format("CC result:         %s", o1.ccFuse(o2).toString()));

        o1 = new SubjectiveOpinion(soPD);
        o2 = new SubjectiveOpinion(soND);
        l.info("----- Two contradicting dogmatic opinions -----");
        l.info(String.format("Fusing opinions    %s and %s", o1.toString(), o2.toString()));
        l.info(String.format("Cumulative result: %s", o1.cumulativeFuse(o2).toString()));
        l.info(String.format("WBF result:        %s", o1.wbFuse(o2).toString()));
        l.info(String.format("CC result:         %s", o1.ccFuse(o2).toString())); // TODO: this result does not seem right

    }


    private List<SubjectiveOpinion> pastMisbehavior() {
        List<SubjectiveOpinion> opinions = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            opinions.add(new SubjectiveOpinion(soP));
        }
        opinions.add(new SubjectiveOpinion(soNN));
        return opinions;
    }
}
