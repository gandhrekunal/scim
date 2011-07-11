/*
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License, Version 1.0 only
 * (the "License").  You may not use this file except in compliance
 * with the License.
 *
 * You can obtain a copy of the license at
 * docs/licenses/cddl.txt
 * or http://www.opensource.org/licenses/cddl1.php.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each
 * file and include the License file at
 * docs/licenses/cddl.txt.  If applicable,
 * add the following below this CDDL HEADER, with the fields enclosed
 * by brackets "[]" replaced with your own identifying information:
 *      Portions Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 *
 *
 *      Copyright 2010-2011 UnboundID Corp.
 */
package com.unboundid.directory.sdk.examples.groovy;



import java.util.List;
import java.util.Random;

import com.unboundid.asn1.ASN1OctetString;
import com.unboundid.directory.sdk.common.types.Entry;
import com.unboundid.directory.sdk.ds.config.PasswordGeneratorConfig;
import com.unboundid.directory.sdk.ds.scripting.ScriptedPasswordGenerator;
import com.unboundid.directory.sdk.ds.types.DirectoryServerContext;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.util.ByteString;
import com.unboundid.util.args.ArgumentException;
import com.unboundid.util.args.ArgumentParser;
import com.unboundid.util.args.IntegerArgument;
import com.unboundid.util.args.StringArgument;



/**
 * This class provides a simple example of a scripted password generator which
 * may be used to generate a password with a specified length using a given
 * character set.  It has two configuration arguments:
 * <UL>
 *   <LI>charset -- The set of characters that may be included in the generated
 *       password.  If this is not specified, then it will default to the set of
 *       lowercase and uppercase ASCII letters and ASCII numeric digits.</LI>
 *   <LI>length -- The number of characters to include in the generated
 *       password.  If this is not specified, then it will default to a value of
 *       eight.</LI>
 * </UL>
 */
public final class ExampleScriptedPasswordGenerator
       extends ScriptedPasswordGenerator
{
  /**
   * The name of the argument that will be used for the argument used to specify
   * the character set for the generated passwords.
   */
  private static final String ARG_NAME_CHARSET = "charset";



  /**
   * The name of the argument that will be used for the argument used to specify
   * the length for generated passwords.
   */
  private static final String ARG_NAME_LENGTH = "length";



  /**
   * The default character set that will be used for generated passwords.
   */
  private static final String DEFAULT_CHARSET =
       "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";



  /**
   * The default length to use for generated passwords.
   */
  private static final int DEFAULT_LENGTH = 8;



  // The set of characters to use in generated passwords.
  private volatile char[] passwordChars;

  // The server context for the server in which this extension is running.
  private DirectoryServerContext serverContext;

  // The length to use for generated passwords.
  private volatile int passwordLength;



  /**
   * Creates a new instance of this password generator.  All password generator
   * implementations must include a default constructor, but any initialization
   * should generally be done in the {@code initializePasswordGenerator} method.
   */
  public ExampleScriptedPasswordGenerator()
  {
    // No implementation required.
  }



  /**
   * Updates the provided argument parser to define any configuration arguments
   * which may be used by this password generator.  The argument parser may also
   * be updated to define relationships between arguments (e.g., to specify
   * required, exclusive, or dependent argument sets).
   *
   * @param  parser  The argument parser to be updated with the configuration
   *                 arguments which may be used by this password generator.
   *
   * @throws  ArgumentException  If a problem is encountered while updating the
   *                             provided argument parser.
   */
  @Override()
  public void defineConfigArguments(final ArgumentParser parser)
         throws ArgumentException
  {
    // Add an argument that allows you to specify the set of allowed characters.
    Character shortIdentifier = null;
    String    longIdentifier  = ARG_NAME_CHARSET;
    boolean   required        = true;
    int       maxOccurrences  = 1;
    String    placeholder     = "{charset}";
    String    description     = "The set of characters that may be included " +
         "in generated passwords.";
    String    defaultValueStr = DEFAULT_CHARSET;

    parser.addArgument(new StringArgument(shortIdentifier, longIdentifier,
         required, maxOccurrences, placeholder, description, defaultValueStr));


    // Add an argument that allows you to specify the length of generated
    // passwords.
    shortIdentifier = null;
    longIdentifier  = ARG_NAME_LENGTH;
    required        = true;
    maxOccurrences  = 1;
    placeholder     = "{length}";
    description     = "The number of characters to include in generated " +
         "passwords.";

    int lowerBound      = 1;
    int upperBound      = Integer.MAX_VALUE;
    int defaultValueInt = DEFAULT_LENGTH;

    parser.addArgument(new IntegerArgument(shortIdentifier, longIdentifier,
         required, maxOccurrences, placeholder, description, lowerBound,
         upperBound, defaultValueInt));
  }



  /**
   * Initializes this password generator.
   *
   * @param  serverContext  A handle to the server context for the server in
   *                        which this extension is running.
   * @param  config         The general configuration for this password
   *                        generator.
   * @param  parser         The argument parser which has been initialized from
   *                        the configuration for this password generator.
   *
   * @throws  LDAPException  If a problem occurs while initializing this
   *                         password generator.
   */
  @Override()
  public void initializePasswordGenerator(
                   final DirectoryServerContext serverContext,
                   final PasswordGeneratorConfig config,
                   final ArgumentParser parser)
         throws LDAPException
  {
    serverContext.debugInfo("Beginning password generator initialization");

    this.serverContext = serverContext;

    // The work we need to do is the same for the initial configuration as for
    // a configuration change, so we'll just call the same method in both cases.
    applyConfig(parser);
  }



  /**
   * Indicates whether the configuration contained in the provided argument
   * parser represents a valid configuration for this extension.
   *
   * @param  config               The general configuration for this password
   *                              generator.
   * @param  parser               The argument parser which has been initialized
   *                              with the proposed configuration.
   * @param  unacceptableReasons  A list that can be updated with reasons that
   *                              the proposed configuration is not acceptable.
   *
   * @return  {@code true} if the proposed configuration is acceptable, or
   *          {@code false} if not.
   */
  @Override()
  public boolean isConfigurationAcceptable(final PasswordGeneratorConfig config,
                      final ArgumentParser parser,
                      final List<String> unacceptableReasons)
  {
    boolean acceptable = true;

    // The argument parser will handle most of the validation.  The only
    // additional validation to perform is to ensure that the proposed character
    // set is not an empty string.
    final StringArgument charsetArg =
         (StringArgument) parser.getNamedArgument(ARG_NAME_CHARSET);
    if ((charsetArg != null) && charsetArg.isPresent())
    {
      final String valueStr = charsetArg.getValue();
      if (valueStr.length() == 0)
      {
        unacceptableReasons.add(
             "The password character set must not be empty.");
        acceptable = false;
      }
    }

    return acceptable;
  }



  /**
   * Attempts to apply the configuration contained in the provided argument
   * parser.
   *
   * @param  config                The general configuration for this password
   *                               generator.
   * @param  parser                The argument parser which has been
   *                               initialized with the new configuration.
   * @param  adminActionsRequired  A list that can be updated with information
   *                               about any administrative actions that may be
   *                               required before one or more of the
   *                               configuration changes will be applied.
   * @param  messages              A list that can be updated with information
   *                               about the result of applying the new
   *                               configuration.
   *
   * @return  A result code that provides information about the result of
   *          attempting to apply the configuration change.
   */
  @Override()
  public ResultCode applyConfiguration(final PasswordGeneratorConfig config,
                                       final ArgumentParser parser,
                                       final List<String> adminActionsRequired,
                                       final List<String> messages)
  {
    // The work we need to do is the same for the initial configuration as for
    // a configuration change, so we'll just call the same method in both cases.
    applyConfig(parser);

    return ResultCode.SUCCESS;
  }



  /**
   * Applies the configuration contained in the provided argument parser.
   *
   * @param  parser  The argument parser with the configuration to apply.
   */
  private void applyConfig(final ArgumentParser parser)
  {
    char[] newCharSet = DEFAULT_CHARSET.toCharArray();
    int    newLength  = DEFAULT_LENGTH;

    final StringArgument charsetArg =
         (StringArgument) parser.getNamedArgument(ARG_NAME_CHARSET);
    if ((charsetArg != null) && charsetArg.isPresent())
    {
      final String valueStr = charsetArg.getValue();
      newCharSet = valueStr.toCharArray();
    }

    final IntegerArgument lengthArg =
         (IntegerArgument) parser.getNamedArgument(ARG_NAME_LENGTH);
    if ((lengthArg != null) && lengthArg.isPresent())
    {
      newLength = lengthArg.getValue();
    }

    passwordChars  = newCharSet;
    passwordLength = newLength;

    serverContext.debugInfo("Set the password character set to " +
         new String(passwordChars));
    serverContext.debugInfo("Set the password length set to " + passwordLength);
  }



  /**
   * Performs any cleanup which may be necessary when this password generator is
   * to be taken out of service.
   */
  @Override()
  public void finalizePasswordGenerator()
  {
    // No finalization is required.
  }



  /**
   * Generates a password for the provided user.
   *
   * @param  userEntry      The entry of the user for whom to generate the
   *                        password.
   *
   * @return  The generated password.
   *
   * @throws  LDAPException  If a problem occurs while attempting to generate a
   *                         password for the user.
   */
  @Override()
  public ByteString generatePassword(final Entry userEntry)
         throws LDAPException
  {
    // Create local copies for the variables to protect against configuration
    // changes while generating a password.
    final char[] chars  = passwordChars;
    final int    length = passwordLength;

    // Create a new random number generator, since they aren't threadsafe.
    // We could use synchronization or thread-locals, but this is good enough.
    final Random random = new Random();

    final StringBuilder buffer = new StringBuilder(length);
    for (int i=0; i < length; i++)
    {
      buffer.append(chars[random.nextInt(chars.length)]);
    }

    return new ASN1OctetString(buffer.toString());
  }
}