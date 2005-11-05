//
// $Id$

package com.threerings.bang.util;

import static com.threerings.bang.Log.log;

/**
 * Instantiates appropriate instances of {@link NameValidator} based on
 * information in the {@link DeploymentConfig}.
 */
public class NameFactory
{
    /**
     * Returns the appropriate instance of {@link NameCreator} for this
     * deployment. <em>Note:</em> only one instance of this class will exist in
     * a JVM and will be shared by all callers.
     */
    public static NameCreator getCreator ()
    {
        if (_creator == null) {
            String defloc = DeploymentConfig.getDefaultLocale();
            if (defloc.equalsIgnoreCase("en")) {
                _creator = new EnglishNameCreator();
            } else {
                log.warning("Unknown default locale '" + defloc + "'.");
                _creator = new EnglishNameCreator();
            }
        }
        return _creator;
    }

    /**
     * Returns the appropriate instance of {@link NameValidator} for this
     * deployment. <em>Note:</em> only one instance of this class will exist in
     * a JVM and will be shared by all callers.
     */
    public static NameValidator getValidator ()
    {
        if (_validator == null) {
            String defloc = DeploymentConfig.getDefaultLocale();
            if (defloc.equalsIgnoreCase("en")) {
                _validator = new EnglishNameValidator();
            } else {
                log.warning("Unknown default locale '" + defloc + "'.");
                _validator = new EnglishNameValidator();
            }
        }
        return _validator;
    }

    protected static NameCreator _creator;
    protected static NameValidator _validator;
}
