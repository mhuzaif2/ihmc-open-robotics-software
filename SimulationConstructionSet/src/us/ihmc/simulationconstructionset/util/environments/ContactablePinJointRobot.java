package us.ihmc.simulationconstructionset.util.environments;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import us.ihmc.simulationconstructionset.GroundContactPoint;
import us.ihmc.simulationconstructionset.Joint;
import us.ihmc.simulationconstructionset.PinJoint;
import us.ihmc.simulationconstructionset.Robot;
import us.ihmc.simulationconstructionset.util.ground.Contactable;
import us.ihmc.robotics.geometry.RigidBodyTransform;

public abstract class ContactablePinJointRobot extends Robot implements Contactable
{
   private final InternalSingleJointArticulatedContactable articulatedContactable;

   public ContactablePinJointRobot(String name)
   {
      super(name);
      articulatedContactable = new InternalSingleJointArticulatedContactable(name, this); 
   }
   
   public abstract PinJoint getPinJoint();
   
   public abstract void getBodyTransformToWorld(RigidBodyTransform transformToWorld);

   public abstract void setMass(double mass);

   public abstract void setMomentOfInertia(double Ixx, double Iyy, double Izz);

   private static class InternalSingleJointArticulatedContactable extends SingleJointArticulatedContactable
   {
      private final ContactablePinJointRobot contactableRobot;

      public InternalSingleJointArticulatedContactable(String name, ContactablePinJointRobot robot)
      {
         super(name, robot);
         this.contactableRobot = robot;
      }

      @Override
      public boolean isClose(Point3d pointInWorldToCheck)
      {
         return contactableRobot.isClose(pointInWorldToCheck);
      }

      @Override
      public boolean isPointOnOrInside(Point3d pointInWorldToCheck)
      {
         return contactableRobot.isPointOnOrInside(pointInWorldToCheck);
      }

      @Override
      public void closestIntersectionAndNormalAt(Point3d intersectionToPack, Vector3d normalToPack, Point3d pointInWorldToCheck)
      {
         contactableRobot.closestIntersectionAndNormalAt(intersectionToPack, normalToPack, pointInWorldToCheck);
      }

      @Override
      public Joint getJoint()
      {
         return contactableRobot.getPinJoint();
      }
   }
   
   public void createAvailableContactPoints(int groupIdentifier, int totalContactPointsAvailable, double forceVectorScale, boolean addDynamicGraphicForceVectorsForceVectors)
   {
      articulatedContactable.createAvailableContactPoints(groupIdentifier, totalContactPointsAvailable, forceVectorScale, addDynamicGraphicForceVectorsForceVectors);
   }

   @Override
   public int getAndLockAvailableContactPoint()
   {
      return articulatedContactable.getAndLockAvailableContactPoint();
   }

   @Override
   public void unlockContactPoint(GroundContactPoint groundContactPoint)
   {
      articulatedContactable.unlockContactPoint(groundContactPoint);
   }

   @Override
   public GroundContactPoint getLockedContactPoint(int contactPointIndex)
   {
      return articulatedContactable.getLockedContactPoint(contactPointIndex);
   }

   @Override
   public void updateContactPoints()
   {
      articulatedContactable.updateContactPoints();
   }


   public void setPosition(double angle)
   {
      getPinJoint().setQ(angle);
   }


   public void setVelocity(double angleDerivative)
   {
      getPinJoint().setQd(angleDerivative);
   }

   public void setPositionAndVelocity(double angle, double angleDerivative)
   {
      setPosition(angle);
      setVelocity(angleDerivative);
   }

   public double getVelocity()
   {
      return getPinJoint().getQDYoVariable().getDoubleValue();
   }

   public double getPosition()
   {
      return getPinJoint().getQYoVariable().getDoubleValue();
   }

}
