package us.ihmc.commonWalkingControlModules.bipedSupportPolygons;

import java.util.ArrayList;
import java.util.List;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;
import us.ihmc.euclid.referenceFrame.FramePoint2D;
import us.ihmc.euclid.referenceFrame.FramePoint3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.euclid.tuple2D.Point2D;
import us.ihmc.euclid.tuple2D.interfaces.Tuple2DBasics;
import us.ihmc.humanoidRobotics.bipedSupportPolygons.ContactablePlaneBody;
import us.ihmc.robotics.geometry.FrameConvexPolygon2d;
import us.ihmc.robotics.screwTheory.MovingReferenceFrame;
import us.ihmc.robotics.screwTheory.RigidBody;

public class ListOfPointsContactablePlaneBody implements ContactablePlaneBody
{
   private final RigidBody rigidBody;
   private final ReferenceFrame soleFrame;
   private final List<Point2D> contactPoints = new ArrayList<Point2D>();
   private final int totalNumberOfContactPoints;

   public ListOfPointsContactablePlaneBody(RigidBody rigidBody, ReferenceFrame soleFrame, List<Point2D> contactPointsInSoleFrame)
   {
      this.rigidBody = rigidBody;
      this.soleFrame = soleFrame;

      for (Point2D contactPoint : contactPointsInSoleFrame)
      {
         this.contactPoints.add(new Point2D(contactPoint));
      }

      totalNumberOfContactPoints = contactPoints.size();
   }

   @Override
   public RigidBody getRigidBody()
   {
      return rigidBody;
   }

   @Override
   public String getName()
   {
      return rigidBody.getName();
   }

   @Override
   public List<FramePoint3D> getContactPointsCopy()
   {
      List<FramePoint3D> ret = new ArrayList<FramePoint3D>(contactPoints.size());
      for (int i = 0; i < contactPoints.size(); i++)
      {
         Tuple2DBasics point = contactPoints.get(i);
         ret.add(new FramePoint3D(soleFrame, point.getX(), point.getY(), 0.0));
      }

      return ret;
   }

   @Override
   public MovingReferenceFrame getFrameAfterParentJoint()
   {
      return rigidBody.getParentJoint().getFrameAfterJoint();
   }

   public FrameConvexPolygon2d getContactPolygonCopy()
   {
      return new FrameConvexPolygon2d(soleFrame, contactPoints);
   }

   @Override
   public ReferenceFrame getSoleFrame()
   {
      return soleFrame;
   }

   @Override
   public List<FramePoint2D> getContactPoints2d()
   {
      List<FramePoint2D> ret = new ArrayList<FramePoint2D>(contactPoints.size());
      for (int i = 0; i < contactPoints.size(); i++)
      {
         Tuple2DBasics point = contactPoints.get(i);
         ret.add(new FramePoint2D(soleFrame, point));
      }

      return ret;
   }

   @Override
   public int getTotalNumberOfContactPoints()
   {
      return totalNumberOfContactPoints;
   }

   @Override
   public void setSoleFrameTransformFromParentJoint(RigidBodyTransform transform)
   {
      throw new NotImplementedException();
   }

}