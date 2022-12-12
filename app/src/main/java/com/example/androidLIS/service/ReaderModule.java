package com.example.androidLIS.service;

import java.util.ArrayList;

/**
 * Created by Bruce_Chiang on 2017/3/16.
 */

public class ReaderModule {

    public static final int CHIP_PL2303 = 1;
    public static final int CHIP_FTXXX = 2;
    private static final ArrayList<int[]> OTGModule;
    static
    {
        OTGModule = new ArrayList();
        OTGModule.add(new int[]{1659, 8963, CHIP_PL2303});      //0x067B, 0x2303
        OTGModule.add(new int[]{1118, 688, CHIP_PL2303});       //0x45E, 0x2B0
        OTGModule.add(new int[]{1027, 24577, CHIP_FTXXX});      //<!-- FT232RL --> 0x0403,0x6001
        OTGModule.add(new int[]{1027, 24596, CHIP_FTXXX});      //<!-- FT232H -->
        OTGModule.add(new int[]{1027, 24592, CHIP_FTXXX});      //<!-- FT2232C/D/HL -->
        OTGModule.add(new int[]{1027, 24593, CHIP_FTXXX});      //<!-- FT4232HL -->
        OTGModule.add(new int[]{1027, 24597, CHIP_FTXXX});      //<!-- FT230X -->
        OTGModule.add(new int[]{1412, 45088, CHIP_FTXXX});      //<!-- REX-USB60F -->
    }

    public static int checkOTGModule(int vid, int pid) {

        for (int i = 0; i < OTGModule.size(); i++) {
            if (OTGModule.get(i)[0] == vid && OTGModule.get(i)[1] == pid) {
                return OTGModule.get(i)[2];
            }
        }
        return -1;
    }

    public enum Type {
        Normal, AA
    };

    public enum Version {
        FI_RXXXX,
        //Add higher version type(2018.10.24)
        FI_R300A_H,
        FI_R300T_H,
        FI_R300V_H,
        FI_R300S_H,


        FI_R3008,
        FI_R300A_C1, FI_R300A_C2,
        FI_R300T_D1, FI_R300T_D2,
        FI_A300S,

        //Add FI_R300A_C3, FI_R300T_D3 type(2017.4.13)
        FI_R300A_C3,
        //FI_R300T_D3,

        //Add FI_R300A_C2C4, FI_R300T_D204(2017.4.21)
        FI_R300A_C2C4,
        FI_R300T_D204,

        //Modify name space FI_R300T_D3 to FI_R300S (2017.5.10)
        FI_R300S,

        //Add Regulation area type EU2 (2017.7.12)
        FI_R300A_C2C5, FI_R300A_C3C5,
        FI_R300T_D205,
        FI_R300S_D305,

        //Add by 2017.9.4
        FI_R300S_D306,
        //FI_R300V_D405,
        FI_R300V_D406,
        FI_R300A_C2C6,

        //Add type (2017.10.11)
        FI_R300T_D206

    };

    public final class Regulation {
        public static final String US = "01";
        public static final String TW = "02";
        public static final String CN = "03";
        public static final String CN2 = "04";
        public static final String EU = "05";
        //Add regulation area(2018.10.24)
        public static final String JP = "06";
        public static final String KR = "07";
        public static final String VN = "08";
        public static final String EU2 = "09";
        public static final String IN = "0A";

        private Regulation() { }
    };

    public final static int FI_R3008_ = 0x0000;
    public final static int FI_R300A_ = 0xC000;
    public final static int FI_R300TSV_ = 0xD000;
    public final static int FI_A300S_ = 0x5000;
    public final static int FI_RXXXX_ = 0xFFFF;


    public static Version check(int ver) {
        switch (ver & 0xF000) {
            case FI_R3008_:
                return Version.FI_R3008;
            case FI_R300A_:
                if ((ver & 0x0F00) == 0x0100)
                    return Version.FI_R300A_C1;
                else if ((ver & 0x0F00) == 0x0200)
                {
                    if ((ver & 0x00FF) < 0x0C4)
                    {
                        return Version.FI_R300A_C2;
                    }
                    else if((ver & 0x00FF) == 0x0C4)
                    {
                        return Version.FI_R300A_C2C4;
                    }
                    else if((ver & 0x00FF) == 0x0C5)
                    {
                        return Version.FI_R300A_C2C5;
                    }
                    else if ((ver & 0x00FF) == 0x0C6)
                    {
                        return Version.FI_R300A_C2C6;
                    }
                    else if ((ver & 0x00FF) > 0x0C6)
                    {
                        return Version.FI_R300A_H;
                    }
                    else
                        return Version.FI_RXXXX;
                }
                else if ((ver & 0x0F00) == 0x0300)
                {
                    if ((ver & 0x00FF) < 0x0C5)
                    {
                        return Version.FI_R300A_C3;
                    }
                    else if ((ver & 0x00FF) == 0x0C5)
                    {
                        return Version.FI_R300A_C3C5;
                    }
                    else if ((ver & 0x00FF) > 0x0C5)
                    {
                        return Version.FI_R300A_H;
                    }
                    else
                        return Version.FI_RXXXX;
                }
                else
                    return Version.FI_RXXXX;
            case FI_R300TSV_:
                if ((ver & 0x0F00) == 0x0100)
                    return Version.FI_R300T_D1;
                else if ((ver & 0x0F00) == 0x0200)
                {
                    if ((ver & 0x00FF) < 0x0004)
                    {
                        return Version.FI_R300T_D2;
                    }
                    else if ((ver & 0x00FF) == 0x0004)
                    {
                        return Version.FI_R300T_D204;
                    }
                    else if ((ver & 0x00FF) == 0x0005)
                    {
                        return Version.FI_R300T_D205;
                    }
                    else if ((ver & 0x00FF) == 0x0006)
                    {
                        return Version.FI_R300T_D206;
                    }
                    else if ((ver & 0x00FF) > 0x0006)
                    {
                        return Version.FI_R300T_H;
                    }
                    else
                    {
                        return Version.FI_RXXXX;
                    }
                }
                else if ((ver & 0x0F00) == 0x0300)
                {
                    if ((ver & 0x00FF) < 0x5)
                    {
                        return Version.FI_R300S;
                    }
                    else if ((ver & 0x00FF) == 0x5)
                    {
                        return Version.FI_R300S_D305;
                    }
                    else if ((ver & 0x00FF) == 0x6)
                    {
                        return Version.FI_R300S_D306;
                    }
                    else if ((ver & 0x00FF) > 0x6)
                    {
                        return Version.FI_R300S_H;
                    }
                    else
                    {
                        return Version.FI_RXXXX;
                    }
                }
                else if ((ver & 0x0F00) == 0x0400)
                {
                    if ((ver & 0x00FF) == 0x5)
                    {
                        //return Version.FI_R300V_D405;
                        return Version.FI_RXXXX;
                    }
                    else if ((ver & 0x00FF) == 0x6)
                    {
                        return Version.FI_R300V_D406;
                    }
                    else if ((ver & 0x00FF) > 0x6)
                    {
                        return Version.FI_R300V_H;
                    }
                    else
                    {
                        return Version.FI_RXXXX;
                    }
                }
                else if ((ver & 0x0F00) >= 0x0500)
                {
                    return Version.FI_R300V_H;
                }
                else
                    return Version.FI_RXXXX;

            case FI_A300S_: return Version.FI_A300S;
            default: return Version.FI_RXXXX;
        }
    }

    public class PowerItem {
        private String name;
        private String value;
        public PowerItem(String n, String v) {
            name = n;
            value = v;
        }
        public String Item() { return name;}
        public String Value() {return value;}
    }

    public static class DataRepository {
        /*public ArrayList<PowerItem> GetPowerGroups(Version v) {
            ArrayList<PowerItem> __list = new ArrayList<PowerItem>();
            int i;

            switch (v) {
                case FI_R300T_D1:
                case FI_R300T_D2:
                    for (i = 27; i >= 0; i-- )
                        __list.add(new PowerItem(Integer.toString(i-2), Integer.toString(i, 16)));
                    break;
                default:
                case FI_R300A_C1:
                case FI_R300A_C2:
                    for (i = 20; i >= 0; i--)
                        __list.add(new PowerItem(Integer.toString(i-2), Integer.toString(i, 16)));
                    break;
                case FI_R300S:
                    for (i = 27; i >= 0; i--)
                        __list.add(new PowerItem(Integer.toString(i), Integer.toString(i, 16)));
                    break;
            }
            return __list;
        }*/
        public static int GetPowerMin(Version v) {
            switch (v) {
                case FI_R300T_D1:
                case FI_R300T_D2:
                case FI_R300T_D204:
                case FI_R300T_D205:
                case FI_R300T_D206:
                case FI_R300T_H:
                    return -2;
                default:
                case FI_R300A_C1:
                case FI_R300A_C2:
                case FI_R300A_C2C4:
                case FI_R300A_C2C5:
                case FI_R300A_C2C6:
                case FI_R300A_C3:
                case FI_R300A_C3C5:
                case FI_R300A_H:
                    return -2;
                case FI_A300S:
                case FI_R300S:
                case FI_R300S_D305:
                case FI_R300S_D306:
                case FI_R300S_H:
                    return 0;
                case FI_R300V_D406:
                case FI_R300V_H:
                    return 2;
            }
        }

        public static int GetPowerMax(Version v) {
            switch (v) {
                case FI_R300T_D1:
                case FI_R300T_D2:
                case FI_R300T_D204:
                case FI_R300T_D205:
                case FI_R300T_D206:
                case FI_R300T_H:
                    return 25;
                default:
                case FI_R300A_C1:
                case FI_R300A_C2:
                case FI_R300A_C2C4:
                case FI_R300A_C2C5:
                case FI_R300A_C2C6:
                case FI_R300A_C3:
                case FI_R300A_C3C5:
                case FI_R300A_H:
                    return 18;
                case FI_A300S:
                case FI_R300S:
                case FI_R300S_D305:
                case FI_R300S_D306:
                case FI_R300S_H:
                    return 27;
                case FI_R300V_D406:
                case FI_R300V_H:
                    return 29;
            }
        }

        public static int GetPowerValue(Version v, int i) {
            switch (v) {
                case FI_R300T_D1:
                case FI_R300T_D2:
                case FI_R300T_D204:
                case FI_R300T_D205:
                case FI_R300T_D206:
                case FI_R300T_H:
                    return i+2;
                default:
                case FI_R300A_C1:
                case FI_R300A_C2:
                case FI_R300A_C2C4:
                case FI_R300A_C2C5:
                case FI_R300A_C2C6:
                case FI_R300A_C3:
                case FI_R300A_C3C5:
                case FI_R300A_H:
                    return i+2;
                case FI_A300S:
                case FI_R300S:
                case FI_R300S_D305:
                case FI_R300S_D306:
                case FI_R300S_H:
                case FI_RXXXX:
                    return i;
                case FI_R300V_D406:
                case FI_R300V_H:
                    return i-2;
            }
        }
    }
}
