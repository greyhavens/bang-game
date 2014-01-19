//
// $Id$

package com.threerings.bang.server.persist;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.PersistenceContext;

import com.threerings.underwire.server.persist.UnderwireRepository;

/**
 * An injectable Underwire repository.
 */
@Singleton
public class BangUnderwireRepository extends UnderwireRepository
{
    @Inject public BangUnderwireRepository (PersistenceContext ctx)
    {
        super(ctx);
    }
}
