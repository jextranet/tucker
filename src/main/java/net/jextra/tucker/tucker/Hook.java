package net.jextra.tucker.tucker;

public interface Hook
{
    Node doHook( HookContext context )
        throws Exception;
}
