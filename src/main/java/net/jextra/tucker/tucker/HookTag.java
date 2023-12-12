/*
 * Copyright (C) Exact Sciences
 * All Rights Reserved.
 */

package net.jextra.tucker.tucker;

import java.lang.annotation.*;

@Retention( RetentionPolicy.RUNTIME )
public @interface HookTag
{
    // Tag name to bind to auto-bind to when registering the Hook automatically with classpath scanning.
    String value();
}
