/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.ldap;

import com.unboundid.ldap.sdk.AbstractConnectionPool;
import com.unboundid.ldap.sdk.BindRequest;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPConnectionPool;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SimpleBindRequest;
import com.unboundid.ldap.sdk.SingleServerSet;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;



/**
 * This class is used to interact with an external LDAP Directory Server.
 */
public class LDAPExternalServer
{
  /**
   * A reference to an LDAP connection pool that may be used to
   * interact with the LDAP server.
   */
  private final AtomicReference<AbstractConnectionPool> connPool;

  /**
   * The configuration of the SCIM server referencing this LDAP server.
   */
  private SCIMServerConfig config;

  /**
   * The set of result codes that will cause an LDAP connection to be considered
   * defunct.
   */
  private static final Set<ResultCode> defunctResultCodes;
  static
  {
    defunctResultCodes = new HashSet<ResultCode>();
    defunctResultCodes.add(ResultCode.OPERATIONS_ERROR);
    defunctResultCodes.add(ResultCode.PROTOCOL_ERROR);
    defunctResultCodes.add(ResultCode.BUSY);
    defunctResultCodes.add(ResultCode.UNAVAILABLE);
    defunctResultCodes.add(ResultCode.UNWILLING_TO_PERFORM);
    defunctResultCodes.add(ResultCode.OTHER);
    defunctResultCodes.add(ResultCode.SERVER_DOWN);
    defunctResultCodes.add(ResultCode.LOCAL_ERROR);
    defunctResultCodes.add(ResultCode.ENCODING_ERROR);
    defunctResultCodes.add(ResultCode.DECODING_ERROR);
    defunctResultCodes.add(ResultCode.NO_MEMORY);
    defunctResultCodes.add(ResultCode.CONNECT_ERROR);
  }





  /**
   * Create a new instance of an LDAP external server from the provided
   * information.
   *
   * @param config  The configuration of the SCIM server referencing this
   *                LDAP server.
   */
  public LDAPExternalServer(final SCIMServerConfig config)
  {
    this.config = config;
    this.connPool = new AtomicReference<AbstractConnectionPool>();
  }



  /**
   * Closes all connections to the LDAP server.
   */
  public void close()
  {
    final AbstractConnectionPool connectionPool = connPool.getAndSet(null);
    if (connectionPool != null)
    {
      connectionPool.close();
    }
  }



  /**
   * Processes a search operation with the provided information against the LDAP
   * external server.  It is expected that at most one entry will be returned
   * from the search, and that no additional content from the successful search
   * result (e.g., diagnostic message or response controls) are needed.
   *
   * @param  searchRequest  The search request to be processed.  If it is
   *                        configured with a search result listener or a size
   *                        limit other than one, then the provided request will
   *                        be duplicated with the appropriate settings.
   *
   * @return  The entry that was returned from the search, or {@code null} if no
   *          entry was returned or the base entry does not exist.
   *
   * @throws  LDAPException        If the search does not complete successfully,
   *                               if more than a single entry is returned, or
   *                               if a problem is encountered while parsing the
   *                               provided filter string, sending the request,
   *                               or reading the response.
   */
  public SearchResultEntry searchForEntry(final SearchRequest searchRequest)
      throws LDAPException
  {
    final AbstractConnectionPool pool = getLDAPConnectionPool();
    final LDAPConnection conn = pool.getConnection();

    boolean failed = true;
    try
    {
      final SearchResultEntry searchResultEntry =
          pool.searchForEntry(searchRequest);
      failed = false;
      return searchResultEntry;
    }
    catch (LDAPException le)
    {
      failed = defunctResultCodes.contains(le.getResultCode());
      throw le;
    }
    finally
    {
      if (failed)
      {
        pool.releaseDefunctConnection(conn);
      }
      else
      {
        pool.releaseConnection(conn);
      }
    }
  }



  /**
   * Retrieves the connection pool that may be used for LDAP operations.
   *
   * @return  The connection pool that may be used for LDAP operations.
   *
   * @throws LDAPException  If the pool is not already connected and a new pool
   *                        cannot be created.
   */
  private AbstractConnectionPool getLDAPConnectionPool()
      throws LDAPException
  {
    AbstractConnectionPool p = connPool.get();

    if ((p != null) && p.isClosed())
    {
      connPool.compareAndSet(p, null);
      p = null;
    }

    if (p == null)
    {
      p = createPool();

      if (! connPool.compareAndSet(null, p))
      {
        p.close();
        return connPool.get();
      }
    }

    return p;
  }



  /**
   * Creates a new LDAP connection pool.
   *
   * @return  The created LDAP connection pool.
   *
   * @throws  LDAPException  If a problem occurs while creating the connection
   *                         pool.
   */
  private AbstractConnectionPool createPool()
      throws LDAPException
  {
    final SingleServerSet dsServerSet =
        new SingleServerSet(config.getDsHost(),
                            config.getDsPort());
    final BindRequest bindRequest =
        new SimpleBindRequest(config.getDsBindDN(),
                              config.getDsBindPassword());
    return new LDAPConnectionPool(dsServerSet, bindRequest,
                                  config.getMaxThreads());
  }
}
