package me.gm.cleaner.xposed.hooker;


import java.util.HashSet;
import java.util.Set;

import me.gm.cleaner.xposed.XposedContext;

public class ContentFilter extends XposedContext implements IHooker {
    private final Set<Object> mIds = new HashSet<>();

    public void hook() throws Throwable {

    }
}
