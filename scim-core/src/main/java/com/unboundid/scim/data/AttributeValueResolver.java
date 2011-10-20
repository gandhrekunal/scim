/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */
package com.unboundid.scim.data;

import com.unboundid.scim.schema.AttributeDescriptor;
import com.unboundid.scim.sdk.SCIMAttributeValue;

import java.util.Date;

/**
 * Used to resolve SCIM attribute values to Java instances.
 *
 * @param <T> The Java class to resolve.
 */
public abstract class AttributeValueResolver<T>
{
  /**
   * Create an instance from the given attribute value.
   *
   * @param value The value to create an instance from.
   * @return The instance created from the attribute value.
   */
  public abstract T toInstance(final SCIMAttributeValue value);

  /**
   * Create a SCIM attribute value from the given instance.
   *
   * @param attributeDescriptor The descriptor for the attribute to create.
   * @param value The instance.
   * @return The SCIM attribute value created from the instance.
   */
  public abstract SCIMAttributeValue fromInstance(
      final AttributeDescriptor attributeDescriptor, final T value);

  /**
   * The <code>AttributeValueResolver</code> that resolves SCIM attribute values
   * to/from <code>String</code> instances.
   */
  public static final AttributeValueResolver<String> STRING_RESOLVER =
      new AttributeValueResolver<String>() {

        /**
         * {@inheritDoc}
         */
        public String toInstance(final SCIMAttributeValue value) {
          return value.getStringValue();
        }

        /**
         * {@inheritDoc}
         */
        public SCIMAttributeValue fromInstance(
            final AttributeDescriptor attributeDescriptor, final String value) {
          return SCIMAttributeValue.createStringValue(value);
        }
      };


  /**
   * The <code>AttributeValueResolver</code> that resolves SCIM attribute values
   * to/from <code>String</code> instances.
   */
  public static final AttributeValueResolver<Date> DATE_RESOLVER =
      new AttributeValueResolver<Date>() {
        /**
         * {@inheritDoc}
         */
        public Date toInstance(final SCIMAttributeValue value) {
          return value.getDateValue();
        }

        /**
         * {@inheritDoc}
         */
        public SCIMAttributeValue fromInstance(
            final AttributeDescriptor attributeDescriptor, final Date value) {
          return SCIMAttributeValue.createDateValue(value);
        }
      };

  /**
   * The <code>AttributeValueResolver</code> that resolves SCIM attribute values
   * to/from <code>Boolean</code> instances.
   */
  public static final AttributeValueResolver<Boolean> BOOLEAN_RESOLVER =
      new AttributeValueResolver<Boolean>() {
        /**
         * {@inheritDoc}
         */
        public Boolean toInstance(final SCIMAttributeValue value) {
          return value.getBooleanValue();
        }

        /**
         * {@inheritDoc}
         */
        public SCIMAttributeValue fromInstance(
            final AttributeDescriptor attributeDescriptor,
            final Boolean value) {
          return SCIMAttributeValue.createBooleanValue(value);
        }
      };


}