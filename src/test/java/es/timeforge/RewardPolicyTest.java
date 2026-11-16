package es.timeforge;

import es.timeforge.service.RewardPolicy;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RewardPolicyTest {
    @Test void noRewardWithoutTimerOrBeforeFiveMinutes(){assertEquals(0,RewardPolicy.calculate(30,600,false,0));assertEquals(0,RewardPolicy.calculate(30,299,true,0));assertEquals(60,RewardPolicy.calculate(30,300,true,0));}
    @Test void onTimeGetsFullReward(){assertEquals(100,RewardPolicy.calculate(50,3000,true,0));}
    @Test void fivePercentBlocksReduceTwoPercent(){assertEquals(100,RewardPolicy.calculate(50,3149,true,0));assertEquals(98,RewardPolicy.calculate(50,3150,true,0));assertEquals(96,RewardPolicy.calculate(50,3300,true,0));}
    @Test void rewardFloorIsTwentyPercent(){assertEquals(20,RewardPolicy.calculate(50,30000,true,0));}
    @Test void dailyCapClipsReward(){assertEquals(10,RewardPolicy.calculate(50,3000,true,1490));assertEquals(0,RewardPolicy.calculate(50,3000,true,1500));}
}
