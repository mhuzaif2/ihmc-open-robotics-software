package controller_msgs.msg.dds;

import us.ihmc.euclid.interfaces.EpsilonComparable;
import us.ihmc.euclid.interfaces.Settable;

/**
 * This message is part of the IHMC whole-body controller API.
 * This message allows the user to precisely select what component in taskspace are to be achieved.
 */
public class SelectionMatrix3DMessage implements Settable<SelectionMatrix3DMessage>, EpsilonComparable<SelectionMatrix3DMessage>
{
   /**
    * The ID of the reference frame defining the selection frame.
    * When selecting the axes of interest, these axes refer to the selection frame axes.
    * This frame is optional. It is preferable to provide it when possible, but when it is absent, i.e. equal to 0,
    * the selection matrix will then be generated regardless to what frame is it used in.
    */
   private long selection_frame_id_;
   /**
    * Specifies whether the x-axis of the selection frame is an axis of interest.
    */
   private boolean x_selected_ = true;
   /**
    * Specifies whether the y-axis of the selection frame is an axis of interest.
    */
   private boolean y_selected_ = true;
   /**
    * Specifies whether the z-axis of the selection frame is an axis of interest.
    */
   private boolean z_selected_ = true;

   public SelectionMatrix3DMessage()
   {

   }

   public SelectionMatrix3DMessage(SelectionMatrix3DMessage other)
   {
      set(other);
   }

   public void set(SelectionMatrix3DMessage other)
   {
      selection_frame_id_ = other.selection_frame_id_;

      x_selected_ = other.x_selected_;

      y_selected_ = other.y_selected_;

      z_selected_ = other.z_selected_;
   }

   /**
    * The ID of the reference frame defining the selection frame.
    * When selecting the axes of interest, these axes refer to the selection frame axes.
    * This frame is optional. It is preferable to provide it when possible, but when it is absent, i.e. equal to 0,
    * the selection matrix will then be generated regardless to what frame is it used in.
    */
   public long getSelectionFrameId()
   {
      return selection_frame_id_;
   }

   /**
    * The ID of the reference frame defining the selection frame.
    * When selecting the axes of interest, these axes refer to the selection frame axes.
    * This frame is optional. It is preferable to provide it when possible, but when it is absent, i.e. equal to 0,
    * the selection matrix will then be generated regardless to what frame is it used in.
    */
   public void setSelectionFrameId(long selection_frame_id)
   {
      selection_frame_id_ = selection_frame_id;
   }

   /**
    * Specifies whether the x-axis of the selection frame is an axis of interest.
    */
   public boolean getXSelected()
   {
      return x_selected_;
   }

   /**
    * Specifies whether the x-axis of the selection frame is an axis of interest.
    */
   public void setXSelected(boolean x_selected)
   {
      x_selected_ = x_selected;
   }

   /**
    * Specifies whether the y-axis of the selection frame is an axis of interest.
    */
   public boolean getYSelected()
   {
      return y_selected_;
   }

   /**
    * Specifies whether the y-axis of the selection frame is an axis of interest.
    */
   public void setYSelected(boolean y_selected)
   {
      y_selected_ = y_selected;
   }

   /**
    * Specifies whether the z-axis of the selection frame is an axis of interest.
    */
   public boolean getZSelected()
   {
      return z_selected_;
   }

   /**
    * Specifies whether the z-axis of the selection frame is an axis of interest.
    */
   public void setZSelected(boolean z_selected)
   {
      z_selected_ = z_selected;
   }

   @Override
   public boolean epsilonEquals(SelectionMatrix3DMessage other, double epsilon)
   {
      if (other == null)
         return false;
      if (other == this)
         return true;

      if (!us.ihmc.idl.IDLTools.epsilonEqualsPrimitive(this.selection_frame_id_, other.selection_frame_id_, epsilon))
         return false;

      if (!us.ihmc.idl.IDLTools.epsilonEqualsBoolean(this.x_selected_, other.x_selected_, epsilon))
         return false;

      if (!us.ihmc.idl.IDLTools.epsilonEqualsBoolean(this.y_selected_, other.y_selected_, epsilon))
         return false;

      if (!us.ihmc.idl.IDLTools.epsilonEqualsBoolean(this.z_selected_, other.z_selected_, epsilon))
         return false;

      return true;
   }

   @Override
   public boolean equals(Object other)
   {
      if (other == null)
         return false;
      if (other == this)
         return true;
      if (!(other instanceof SelectionMatrix3DMessage))
         return false;

      SelectionMatrix3DMessage otherMyClass = (SelectionMatrix3DMessage) other;

      if (this.selection_frame_id_ != otherMyClass.selection_frame_id_)
         return false;

      if (this.x_selected_ != otherMyClass.x_selected_)
         return false;

      if (this.y_selected_ != otherMyClass.y_selected_)
         return false;

      if (this.z_selected_ != otherMyClass.z_selected_)
         return false;

      return true;
   }

   @Override
   public java.lang.String toString()
   {
      StringBuilder builder = new StringBuilder();

      builder.append("SelectionMatrix3DMessage {");
      builder.append("selection_frame_id=");
      builder.append(this.selection_frame_id_);

      builder.append(", ");
      builder.append("x_selected=");
      builder.append(this.x_selected_);

      builder.append(", ");
      builder.append("y_selected=");
      builder.append(this.y_selected_);

      builder.append(", ");
      builder.append("z_selected=");
      builder.append(this.z_selected_);

      builder.append("}");
      return builder.toString();
   }
}