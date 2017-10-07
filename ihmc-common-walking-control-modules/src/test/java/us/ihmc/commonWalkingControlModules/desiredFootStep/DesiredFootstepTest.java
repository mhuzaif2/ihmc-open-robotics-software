package us.ihmc.commonWalkingControlModules.desiredFootStep;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import us.ihmc.communication.net.NetClassList;
import us.ihmc.communication.net.PacketConsumer;
import us.ihmc.communication.packetCommunicator.PacketCommunicator;
import us.ihmc.communication.packets.Packet;
import us.ihmc.communication.packets.PacketDestination;
import us.ihmc.communication.util.NetworkPorts;
import us.ihmc.continuousIntegration.ContinuousIntegrationAnnotations.ContinuousIntegrationPlan;
import us.ihmc.continuousIntegration.ContinuousIntegrationAnnotations.ContinuousIntegrationTest;
import us.ihmc.continuousIntegration.IntegrationCategory;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.tuple2D.Point2D;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.euclid.tuple4D.Quaternion;
import us.ihmc.communication.packets.ExecutionMode;
import us.ihmc.communication.packets.ExecutionTiming;
import us.ihmc.humanoidRobotics.communication.packets.walking.FootstepDataListMessage;
import us.ihmc.humanoidRobotics.communication.packets.walking.FootstepDataMessage;
import us.ihmc.humanoidRobotics.communication.packets.walking.FootstepStatus;
import us.ihmc.humanoidRobotics.communication.packets.walking.PauseWalkingMessage;
import us.ihmc.humanoidRobotics.footstep.Footstep;
import us.ihmc.robotics.geometry.FramePose;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.trajectories.TrajectoryType;
import us.ihmc.tools.MemoryTools;
import us.ihmc.tools.thread.ThreadTools;

@ContinuousIntegrationPlan(categories = IntegrationCategory.FAST)
public class DesiredFootstepTest
{
   private static final RobotSide robotSide = RobotSide.LEFT;
   //TODO: Make listeners or something blocking so we don't have to do arbitrary sleep times...
   private static final long SLEEP_TIME = 500;

   @Before
   public void showMemoryUsageBeforeTest()
   {
      MemoryTools.printCurrentMemoryUsageAndReturnUsedMemoryInMB(getClass().getSimpleName() + " before test.");
   }

   @After
   public void showMemoryUsageAfterTest()
   {
      MemoryTools.printCurrentMemoryUsageAndReturnUsedMemoryInMB(getClass().getSimpleName() + " after test.");
   }

   /**
    * This test verifies that FootstepData can be sent and received using our current message passing utilities
    * @throws IOException
    */

   @ContinuousIntegrationTest(estimatedDuration = 1.6, categoriesOverride = IntegrationCategory.FLAKY)
   @Test(timeout = 30000)
   public void testPassingFootstepData() throws IOException
   {
      Random random = new Random(5642769L);

      // setup comms
      NetworkPorts port = NetworkPorts.createRandomTestPort(random);
      //      QueueBasedStreamingDataProducer<FootstepData> queueBasedStreamingDataProducer = new QueueBasedStreamingDataProducer<FootstepData>("FootstepData");
      PacketCommunicator tcpServer = createAndStartStreamingDataTCPServer(port);
      FootstepDataConsumer footstepDataConsumer = new FootstepDataConsumer();
      PacketCommunicator tcpClient = createStreamingDataConsumer(FootstepDataMessage.class, footstepDataConsumer, port);
      ThreadTools.sleep(SLEEP_TIME);
      //      queueBasedStreamingDataProducer.startProducingData();

      // create test footsteps
      ArrayList<Footstep> sentFootsteps = createRandomFootsteps(50);
      for (Footstep footstep : sentFootsteps)
      {
         FootstepDataMessage footstepData = new FootstepDataMessage(footstep);
         tcpServer.send(footstepData);
         //         queueBasedStreamingDataProducer.queueDataToSend(footstepData);
      }

      ThreadTools.sleep(SLEEP_TIME);

      tcpClient.disconnect();
      tcpServer.disconnect();

      // verify received correctly
      ArrayList<Footstep> receivedFootsteps = footstepDataConsumer.getReconstructedFootsteps();

      compareFootstepsSentWithReceived(sentFootsteps, receivedFootsteps);
   }

   @ContinuousIntegrationTest(estimatedDuration = 1.4)
   @Test(timeout = 30000)
   public void testPassingFootstepPath() throws IOException
   {
      Random random = new Random(1582l);
      // setup comms
      NetworkPorts port = NetworkPorts.createRandomTestPort(random);
      //      QueueBasedStreamingDataProducer<FootstepDataList> queueBasedStreamingDataProducer = new QueueBasedStreamingDataProducer<FootstepDataList>("FootstepDataList");
      PacketCommunicator tcpServer = createAndStartStreamingDataTCPServer(port);

      FootstepPathConsumer footstepPathConsumer = new FootstepPathConsumer();
      PacketCommunicator tcpClient = createStreamingDataConsumer(FootstepDataListMessage.class, footstepPathConsumer, port);
      ThreadTools.sleep(SLEEP_TIME);
      //      queueBasedStreamingDataProducer.startProducingData();

      // create test footsteps
      ArrayList<Footstep> sentFootsteps = createRandomFootsteps(50);
      FootstepDataListMessage footstepsData = convertFootstepsToFootstepData(sentFootsteps, random.nextDouble(), random.nextDouble());

      tcpServer.send(footstepsData);
      ThreadTools.sleep(SLEEP_TIME);

      tcpClient.disconnect();
      tcpServer.disconnect();

      // verify received correctly
      ArrayList<Footstep> receivedFootsteps = footstepPathConsumer.getReconstructedFootsteps();
      compareFootstepsSentWithReceived(sentFootsteps, receivedFootsteps);
   }

   @ContinuousIntegrationTest(estimatedDuration = 1.6, categoriesOverride = IntegrationCategory.FLAKY)
   @Test(timeout = 30000)
   public void testPassingPauseCommand() throws IOException
   {
      Random random = new Random(5642568L);

      // setup comms
      NetworkPorts port = NetworkPorts.createRandomTestPort(random);
      //      QueueBasedStreamingDataProducer<PauseCommand> queueBasedStreamingDataProducer = new QueueBasedStreamingDataProducer<PauseCommand>("PauseCommand");
      PacketCommunicator tcpServer = createAndStartStreamingDataTCPServer(port);

      PauseConsumer pauseConsumer = new PauseConsumer();
      PacketCommunicator tcpClient = createStreamingDataConsumer(PauseWalkingMessage.class, pauseConsumer, port);
      ThreadTools.sleep(SLEEP_TIME);
      //      queueBasedStreamingDataProducer.startProducingData();

      // create test commands
      ArrayList<Boolean> commands = new ArrayList<Boolean>();
      int numberToTest = 100;
      for (int i = 0; i < numberToTest; i++)
      {
         boolean isPaused = random.nextBoolean();
         commands.add(isPaused);
         tcpServer.send(new PauseWalkingMessage(isPaused));
      }

      ThreadTools.sleep(SLEEP_TIME);

      tcpServer.disconnect();
      tcpClient.disconnect();

      // verify received correctly
      ArrayList<Boolean> reconstructedCommands = pauseConsumer.getReconstructedCommands();
      for (int i = 0; i < commands.size(); i++)
      {
         Boolean isPaused = commands.get(i);
         Boolean reconstructedCommand = reconstructedCommands.get(i);
         assertTrue(isPaused.booleanValue() == reconstructedCommand.booleanValue());
      }
   }

   @ContinuousIntegrationTest(estimatedDuration = 2.6)
   @Test(timeout = 30000)
   public void testPassingFootstepPathAndPauseCommands() throws IOException
   {
      Random random = new Random(5632469L);

      // Create one server for two types of data
      NetworkPorts pathPort = NetworkPorts.createRandomTestPort(random);

      //      QueueBasedStreamingDataProducer<FootstepDataList> pathQueueBasedStreamingDataProducer = new QueueBasedStreamingDataProducer<FootstepDataList>("FootstepDataList");

      //      QueueBasedStreamingDataProducer<PauseCommand> pauseQueueBasedStreamingDataProducer = new QueueBasedStreamingDataProducer<PauseCommand>("PauseCommand");

      PacketCommunicator streamingDataTCPServer = createAndStartStreamingDataTCPServer(pathPort);
      //      pauseQueueBasedStreamingDataProducer.addConsumer(streamingDataTCPServer);

      // create one client for two types of data
      FootstepPathConsumer footstepPathConsumer = new FootstepPathConsumer();
      PauseConsumer pauseConsumer = new PauseConsumer();

      PacketCommunicator streamingDataTCPClient = createStreamingDataConsumer(FootstepDataListMessage.class, footstepPathConsumer, pathPort);
      streamingDataTCPClient.attachListener(PauseWalkingMessage.class, pauseConsumer);

      ThreadTools.sleep(SLEEP_TIME);
      //      pathQueueBasedStreamingDataProducer.startProducingData();
      //      pauseQueueBasedStreamingDataProducer.startProducingData();

      // send test footstep path
      ArrayList<Footstep> sentFootsteps = createRandomFootsteps(50);
      FootstepDataListMessage footstepsData = convertFootstepsToFootstepData(sentFootsteps, random.nextDouble(), random.nextDouble());

      streamingDataTCPServer.send(footstepsData);
      ThreadTools.sleep(SLEEP_TIME);

      // send some commands
      ArrayList<Boolean> commands = new ArrayList<Boolean>();
      int numberToTest = 3;
      for (int i = 0; i < numberToTest; i++)
      {
         boolean isPaused = random.nextBoolean();
         commands.add(isPaused);
         streamingDataTCPServer.send(new PauseWalkingMessage(isPaused));
      }

      ThreadTools.sleep(SLEEP_TIME);

      // send another footstep path
      ArrayList<Footstep> sentFootsteps2 = createRandomFootsteps(50);
      footstepsData = convertFootstepsToFootstepData(sentFootsteps2, random.nextDouble(), random.nextDouble());

      streamingDataTCPServer.send(footstepsData);
      sentFootsteps.addAll(sentFootsteps2);
      ThreadTools.sleep(SLEEP_TIME);

      streamingDataTCPClient.disconnect();
      streamingDataTCPServer.disconnect();

      // verify footsteps received correctly
      ArrayList<Footstep> receivedFootsteps = footstepPathConsumer.getReconstructedFootsteps();
      compareFootstepsSentWithReceived(sentFootsteps, receivedFootsteps);

      // verify commands received correctly
      ArrayList<Boolean> reconstructedCommands = pauseConsumer.getReconstructedCommands();
      for (int i = 0; i < commands.size(); i++)
      {
         Boolean isPaused = commands.get(i);
         Boolean reconstructedCommand = reconstructedCommands.get(i);
         assertTrue(isPaused.booleanValue() == reconstructedCommand.booleanValue());
      }
   }

   @ContinuousIntegrationTest(estimatedDuration = 1.6)
   @Test(timeout = 30000)
   public void testPassingFootstepStatus() throws IOException
   {
      Random random = new Random(3642569L);

      // setup comms
      NetworkPorts port = NetworkPorts.createRandomTestPort(random);
      PacketCommunicator tcpServer = createAndStartStreamingDataTCPServer(port);

      FootstepStatusConsumer footstepStatusConsumer = new FootstepStatusConsumer();
      PacketCommunicator tcpClient = createStreamingDataConsumer(FootstepStatus.class, footstepStatusConsumer, port);
      ThreadTools.sleep(SLEEP_TIME);

      // create test footsteps
      ArrayList<FootstepStatus> sentFootstepStatus = new ArrayList<FootstepStatus>();
      for (int i = 0; i < 50; i++)
      {
         FootstepStatus.Status status = FootstepStatus.Status.STARTED;
         boolean isComplete = random.nextBoolean();
         if (isComplete)
         {
            status = FootstepStatus.Status.COMPLETED;
         }

         FootstepStatus footstepStatus = new FootstepStatus(status, i);
         sentFootstepStatus.add(footstepStatus);
         tcpServer.send(footstepStatus);
      }

      ThreadTools.sleep(SLEEP_TIME);

      tcpServer.disconnect();
      tcpClient.disconnect();

      // verify received correctly
      ArrayList<FootstepStatus> receivedFootsteps = footstepStatusConsumer.getReconstructedFootsteps();
      compareStatusSentWithReceived(sentFootstepStatus, receivedFootsteps);
   }

   private NetClassList getNetClassList()
   {
      NetClassList netClassList = new NetClassList();
      netClassList.registerPacketClass(FootstepDataMessage.class);
      netClassList.registerPacketClass(FootstepDataListMessage.class);
      netClassList.registerPacketClass(PauseWalkingMessage.class);
      netClassList.registerPacketClass(FootstepStatus.class);
      netClassList.registerPacketClass(ExecutionMode.class);
      netClassList.registerPacketClass(ExecutionTiming.class);

      netClassList.registerPacketField(ArrayList.class);
      netClassList.registerPacketField(Point3D.class);
      netClassList.registerPacketField(Point3D[].class);
      netClassList.registerPacketField(Quaternion.class);
      netClassList.registerPacketField(PacketDestination.class);
      netClassList.registerPacketField(FootstepStatus.Status.class);
      netClassList.registerPacketField(TrajectoryType.class);
      netClassList.registerPacketField(RobotSide.class);

      return netClassList;
   }

   private PacketCommunicator createAndStartStreamingDataTCPServer(NetworkPorts port) throws IOException
   {

      PacketCommunicator server = PacketCommunicator.createTCPPacketCommunicatorServer(port, getNetClassList());
      server.connect();
      //      queueBasedStreamingDataProducer.addConsumer(server);
      return server;
   }

   private <T extends Packet> PacketCommunicator createStreamingDataConsumer(Class<T> clazz, PacketConsumer<T> consumer, NetworkPorts port) throws IOException
   {
      PacketCommunicator client = PacketCommunicator.createTCPPacketCommunicatorClient("localhost", port, getNetClassList());
      client.connect();
      client.attachListener(clazz, consumer);
      return client;
   }

   private ArrayList<Footstep> createRandomFootsteps(int number)
   {
      Random random = new Random(77);
      ArrayList<Footstep> footsteps = new ArrayList<Footstep>();

      for (int footstepNumber = 0; footstepNumber < number; footstepNumber++)
      {
         FramePose pose = new FramePose(ReferenceFrame.getWorldFrame(), new Point3D(footstepNumber, 0.0, 0.0),
               new Quaternion(random.nextDouble(), random.nextDouble(), random.nextDouble(), random.nextDouble()));

         boolean trustHeight = true;
         Footstep footstep = new Footstep(robotSide, pose, trustHeight);
         footsteps.add(footstep);
      }

      return footsteps;
   }

   private void compareFootstepsSentWithReceived(ArrayList<Footstep> sentFootsteps, ArrayList<Footstep> receivedFootsteps)
   {
      for (int i = 0; i < sentFootsteps.size(); i++)
      {
         Footstep sentFootstep = sentFootsteps.get(i);
         Footstep receivedFootstep = receivedFootsteps.get(i);

         if (!sentFootstep.epsilonEquals(receivedFootstep, 1e-4))
         {
            System.out.println("Test Broken");
         }
         assertTrue(sentFootstep.epsilonEquals(receivedFootstep, 1e-4));
      }
   }

   private void compareStatusSentWithReceived(ArrayList<FootstepStatus> sentFootstepStatus, ArrayList<FootstepStatus> receivedFootsteps)
   {
      for (int i = 0; i < sentFootstepStatus.size(); i++)
      {
         FootstepStatus footstepStatus = sentFootstepStatus.get(i);
         FootstepStatus reconstructedFootstepStatus = receivedFootsteps.get(i);
         assertTrue(footstepStatus.getStatus() == reconstructedFootstepStatus.getStatus());
      }
   }

   private static FootstepDataListMessage convertFootstepsToFootstepData(ArrayList<Footstep> footsteps, double swingTime, double transferTime)
   {
      FootstepDataListMessage footstepsData = new FootstepDataListMessage(swingTime, transferTime);

      for (Footstep footstep : footsteps)
      {
         footstepsData.add(new FootstepDataMessage(footstep));
      }

      return footstepsData;
   }

   private class FootstepDataConsumer implements PacketConsumer<FootstepDataMessage>
   {
      ArrayList<Footstep> reconstructedFootsteps = new ArrayList<Footstep>();

      @Override
      public void receivedPacket(FootstepDataMessage packet)
      {
         FramePose pose = new FramePose(ReferenceFrame.getWorldFrame(), packet.getLocation(), packet.getOrientation());

         boolean trustHeight = true;
         Footstep footstep = new Footstep(packet.getRobotSide(), pose, trustHeight);
         reconstructedFootsteps.add(footstep);
      }

      public ArrayList<Footstep> getReconstructedFootsteps()
      {
         return reconstructedFootsteps;
      }
   }

   private class FootstepPathConsumer implements PacketConsumer<FootstepDataListMessage>
   {
      ArrayList<Footstep> reconstructedFootstepPath = new ArrayList<Footstep>();

      @Override
      public void receivedPacket(FootstepDataListMessage packet)
      {
         for (FootstepDataMessage footstepData : packet)
         {
            List<Point2D> contactPoints = footstepData.getPredictedContactPoints();
            if (contactPoints != null && contactPoints.size() == 0)
               contactPoints = null;
            FramePose footstepPose = new FramePose(ReferenceFrame.getWorldFrame(), footstepData.getLocation(), footstepData.getOrientation());

            Footstep footstep = new Footstep(robotSide, footstepPose, true, contactPoints);
            footstep.setTrajectoryType(footstepData.getTrajectoryType());
            footstep.setSwingHeight(footstepData.getSwingHeight());
            reconstructedFootstepPath.add(footstep);
         }
      }

      public ArrayList<Footstep> getReconstructedFootsteps()
      {
         return reconstructedFootstepPath;
      }
   }

   private class PauseConsumer implements PacketConsumer<PauseWalkingMessage>
   {
      ArrayList<Boolean> reconstructedCommands = new ArrayList<Boolean>();

      @Override
      public void receivedPacket(PauseWalkingMessage packet)
      {
         reconstructedCommands.add(packet.isPaused());
      }

      public ArrayList<Boolean> getReconstructedCommands()
      {
         return reconstructedCommands;
      }
   }

   private class FootstepStatusConsumer implements PacketConsumer<FootstepStatus>
   {
      private final ArrayList<FootstepStatus> reconstructedFootstepStatuses = new ArrayList<FootstepStatus>();

      @Override
      public void receivedPacket(FootstepStatus footstepStatus)
      {
         reconstructedFootstepStatuses.add(footstepStatus);
      }

      public ArrayList<FootstepStatus> getReconstructedFootsteps()
      {
         return reconstructedFootstepStatuses;
      }
   }
}
