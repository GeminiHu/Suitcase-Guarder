package edu.ucr.ece.btdatacomm;

/**
 * Created by yhu on 8/18/2016.
 */
public class HomeConfig {
    /**服务器端的IP */
    public static final String SERVER_IP = "192.168.1.158";

    /**服务器端接听的TCP端口 */
    public static final int SERVER_TCP_PORT = 6000;


    /**门继电器状态  false为关, true为开*/
    public static  boolean LOCK_STATUE = false;

    /**房间灯状态  false为关, true为开*/
    public static  boolean TEMPERATURE_SCALE = false;

    /**客厅灯状态  false为关, true为开*/
    public static  boolean SECURITY_STATUE = false;

    /** 窗帘状态 false为关, true为开 */
    public static boolean CURTAIN_STATUE = false;

    /** 空调状态false为关, true为开 */
    public static boolean AIRCONDITIONING_STATUE = false;

    /** 警报状态false为关, true为开 */
    public static boolean ALARM_STATUE = false;
}
