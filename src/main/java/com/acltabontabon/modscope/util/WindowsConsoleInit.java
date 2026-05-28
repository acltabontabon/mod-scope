package com.acltabontabon.modscope.util;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

public final class WindowsConsoleInit {

    private static final int STD_OUTPUT_HANDLE = -11;
    private static final int ENABLE_VIRTUAL_TERMINAL_PROCESSING = 0x0004;

    private WindowsConsoleInit() {}

    public static void apply() {
        if (!System.getProperty("os.name", "").toLowerCase().contains("win")) return;
        try {
            Linker linker = Linker.nativeLinker();
            SymbolLookup kernel32 = SymbolLookup.libraryLookup("kernel32", Arena.global());

            MethodHandle setOutputCP = linker.downcallHandle(
                kernel32.find("SetConsoleOutputCP").orElseThrow(),
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT)
            );
            setOutputCP.invoke(65001); // UTF-8

            MethodHandle getStdHandle = linker.downcallHandle(
                kernel32.find("GetStdHandle").orElseThrow(),
                FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.JAVA_INT)
            );
            MethodHandle getConsoleMode = linker.downcallHandle(
                kernel32.find("GetConsoleMode").orElseThrow(),
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            );
            MethodHandle setConsoleMode = linker.downcallHandle(
                kernel32.find("SetConsoleMode").orElseThrow(),
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT)
            );

            MemorySegment stdOut = (MemorySegment) getStdHandle.invoke(STD_OUTPUT_HANDLE);
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment modePtr = arena.allocate(ValueLayout.JAVA_INT);
                if ((int) getConsoleMode.invoke(stdOut, modePtr) != 0) {
                    int mode = modePtr.get(ValueLayout.JAVA_INT, 0);
                    setConsoleMode.invoke(stdOut, mode | ENABLE_VIRTUAL_TERMINAL_PROCESSING);
                }
            }
        } catch (Throwable ignored) {}
    }
}
