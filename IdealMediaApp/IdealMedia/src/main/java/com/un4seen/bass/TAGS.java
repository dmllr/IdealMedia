package com.un4seen.bass;

import java.nio.ByteBuffer;

public class TAGS
{
	public static native String TAGS_GetLastErrorDesc();
	public static native String TAGS_Read(int dwHandle, String fmt);
	public static native String TAGS_ReadEx(int dwHandle, String fmt, int tagtype);
	public static native ByteBuffer TAGS_ReadExByte(int dwHandle, String fmt, int tagtype);
	public static native int TAGS_GetVersion();
	
    static {
        System.loadLibrary("tags");
    }
}
