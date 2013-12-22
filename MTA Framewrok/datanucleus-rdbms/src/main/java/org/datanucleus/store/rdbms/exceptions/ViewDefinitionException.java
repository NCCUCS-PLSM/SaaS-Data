/**********************************************************************
Copyright (c) 2002 Mike Martin and others. All rights reserved.
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

Contributors:
    Andy Jefferson - coding standards
    ...
**********************************************************************/
package org.datanucleus.store.rdbms.exceptions;

import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.store.rdbms.RDBMSStoreManager;
import org.datanucleus.util.Localiser;

/**
 * A <tt>ViewDefinitionException</tt> is thrown if the metadata extension(s)
 * that define a view are missing or invalid.
 *
 * @see org.datanucleus.store.rdbms.table.ClassView
 * @version $Revision: 1.2 $ 
 */
public class ViewDefinitionException extends NucleusUserException
{
    protected static final Localiser LOCALISER=Localiser.getInstance("org.datanucleus.store.rdbms.Localisation",
        RDBMSStoreManager.class.getClassLoader());

    /**
     * Constructs a class definition exception with the specified detail
     * message.
     * @param className The class name for the class backed by a view.
     * @param viewDef The string provided in the metadata defining the view.
     */
    public ViewDefinitionException(String className, String viewDef)
    {
        super(LOCALISER.msg("020017",className,viewDef));
        setFatal();
    }
}
