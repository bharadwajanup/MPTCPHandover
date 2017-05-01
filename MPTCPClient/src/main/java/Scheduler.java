import network.PacketType;
import network.SocketEndPoint;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.io.IOException;
import java.util.HashMap;

import static java.lang.Double.NaN;

/**
 * Created as part of the class project for Mobile Computing
 */
public class Scheduler {
    public int windowSize;
    private boolean isMainFlow;
    private DescriptiveStatistics dataManager;
    private HashMap<SocketEndPoint, Flow> flowTable;
    private Flow mainFlow;
    private Flow subFlow; //Could Extend to multiple subflows.
    private double magicValue;

    public Scheduler(int windowSize) {
        this.windowSize = windowSize;
        dataManager = new DescriptiveStatistics(windowSize);
        magicValue = 0.2;
        setFlow(true);

    }

    public Flow getMainFlow() {
        return mainFlow;
    }

    public void setFlow(SocketEndPoint endPoint, double expectation, boolean main) {
        if (main)
            this.mainFlow = new Flow(endPoint, expectation);
        else
            this.subFlow = new Flow(endPoint, expectation);
    }

    public Flow getSubFlow() {
        return subFlow;
    }


    private double getChangeInLatency(double mean, double curVal) {
        return (curVal - mean) / mean;
    }

//    public void update(double val) {
//        double curMean = dataManager.getMean();
//
//        dataManager.addValue(val);
//
//        double changeInVal = getChangeInLatency(curMean, val);
//
//        setFlow(changeInVal < magicValue);
//    }

    public boolean isMainFlow() {
        return isMainFlow;
    }

    public void setFlow(boolean mainFlow) {
        isMainFlow = mainFlow;
    }

    public PacketType getPacketType(boolean main) {
        if (main) {
            return isMainFlow() ? PacketType.ACKNOWLEDGEMENT : PacketType.PING;
        }
        return isMainFlow() ? PacketType.PING : PacketType.ACKNOWLEDGEMENT;
    }

    public void updateMainFlow(Double y) {
        getMainFlow().update(y);
        setFlow(getChangeInLatency(getMainFlow().expectation, y) < magicValue);
        getMainFlow().setExpectation(Math.min(getMainFlow().getExpectation(), getMainFlow().dataManager.getMean()));

    }

    public void updateSubFlow(Double y) {
        double prevVal = getSubFlow().dataManager.getMean();
        getSubFlow().update(y);
//        getSubFlow().setExpectation(getSubFlow().dataManager.getMean());
        double change = getChangeInLatency(prevVal, getSubFlow().dataManager.getMean());

        //Adjust Main Flow's expectations.
        if (!Double.isNaN(change)) {
//            System.out.println(change);
            double mainFLowExpectation = (1 + change) * getMainFlow().getExpectation();
            getMainFlow().setExpectation(mainFLowExpectation);
        }

        //If Sub-flow's speed is worse than main flow, switch back.
        if (getSubFlow().dataManager.getMean() >= getMainFlow().dataManager.getMean())
            setFlow(true);
    }

    public void closeFlows() throws IOException {
        getMainFlow().getEndPoint().close();
        getSubFlow().getEndPoint().close();
    }

    public class Flow {
        DescriptiveStatistics dataManager;
        private SocketEndPoint endPoint;
        private double expectation;

        Flow(SocketEndPoint endPoint, double expectation) {
            this.setEndPoint(endPoint);
            this.setExpectation(expectation);
            dataManager = new DescriptiveStatistics(windowSize);
        }

        public SocketEndPoint getEndPoint() {
            return endPoint;
        }

        public void setEndPoint(SocketEndPoint endPoint) {
            this.endPoint = endPoint;
        }

        public double getExpectation() {
            return expectation * (1 + magicValue);
        }

        public void setExpectation(double expectation) {
            if (expectation == NaN) {
                System.out.println("Invalid expectation");
                return;
            }

            this.expectation = expectation;
        }

        public void update(Double val) {
            dataManager.addValue(val);
            expectation = Math.min(dataManager.getMean(), expectation);
        }
    }
}
