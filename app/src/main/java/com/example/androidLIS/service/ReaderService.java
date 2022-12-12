package com.example.androidLIS.service;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.nio.charset.StandardCharsets;

/**
 * FAVEPC Reader Library Document 2017/3/15.
 *
 */

public class ReaderService {
    private StringBuilder mBuilder;

    /**
     * Command stop character
     */
    public static String COMMAND_END = "\r\n";

    /**
     * Multi(U) command stop character
     */
    public static String COMMANDU_END = "\nU\r\n";

    public ReaderService()
    {
        if (this.mBuilder == null)
            this.mBuilder = new StringBuilder();
    }

    /**
     *
     */
    public static class Format {
        //Polynomial: x^8 + x^5 + x^4 + 1 (0x31)<br>
        //Initial value: 0x0
        //Residue: 0x0
        //The following is the equivalent functionality written in c#.
        public static byte crc8(byte[] paramBytes) {
            int crcInit = 0;
            int bitCount;

            if (paramBytes == null) return 0;

            for (int j = 0; j < paramBytes.length; j++) {
                bitCount = 8;
                crcInit ^= paramBytes[j] & 0xFF;
                do {
                    if ((crcInit & 0x80) != 0) crcInit = (crcInit << 1) ^ 0x31;
                    else crcInit <<= 1;
                    bitCount--;
                } while (bitCount > 0);
            }

            return (byte)crcInit;
        }

        //Polynomial: x^16 + x^12 + x^5 + 1 (0x1021)<br>
        //Initial value: 0xFFFF
        //Residue: 0x1D0F
        //The following is the equivalent functionality(ISO/IEC 13239) written in java.
        public static int crc16(byte[] paramBytes) {
            int crc16 = 0xFFFF;
            int polynomial = 0x1021;   // 0001 0000 0010 0001  (0, 5, 12)
            int x=0,y=0;

            if (paramBytes[0] == 0) {
                x=8; y=1;
            }
            for(int i = x, j = y ; i < paramBytes.length*8; i++) {
                if((i%8) == 0){
                    crc16 ^= (paramBytes[j++] << 8) & 0xFF00;
                }
                if((crc16 & 0x8000) != 0)
                    crc16 = (((crc16<<1) & 0xFFFE) ^ polynomial);
                else {
                    crc16 <<= 1;
                    crc16 &= 0xFFFE;
                }
            }
            crc16 &= 0xFFFF;
            return crc16;
        }


        /**
         * Makes up 0 to target string
         * @param str target string
         * @param lenSize the length of string to convert.
         * @return the {#lenSize} of string convert.
         */
        public static String makesUpZero(String str, int lenSize) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < lenSize; i++) sb.append("0");
            String returnValue = sb.toString() + str;

            return returnValue.substring(returnValue.length() - lenSize);
        }


        /**
         * Converts the value of an 8-bit unsigned integer to
         * its equivalent string representation in a specified base.
         * @param bytes the 8-bit unsigned integer to convert.
         * @return The string representation of bytes
         */
        public static String bytesToString(byte[] bytes) {
            return (bytes == null) ? "" : AsciiOctets2String(bytes , false);
        }

        public static String bytesToString(byte[] bytes, boolean b) {
            return (bytes == null) ? "" : AsciiOctets2String(bytes, b);
        }

        static String AsciiOctets2String(byte[] bytes, boolean b) {
            StringBuilder sb = new StringBuilder(bytes.length);

            for(int i = 0; i < bytes.length; i++) {
                char c = (char) (bytes[i] & 0xFF);
                switch (c) {
                    case '\u0000': sb.append("<NUL>"); break;
                    case '\u0001': sb.append("<SOH>"); break;
                    case '\u0002': sb.append("<STX>"); break;
                    case '\u0003': sb.append("<ETX>"); break;
                    case '\u0004': sb.append("<EOT>"); break;
                    case '\u0005': sb.append("<ENQ>"); break;
                    case '\u0006': sb.append("<ACK>"); break;
                    case '\u0007': sb.append("<BEL>"); break;
                    case '\u0008': sb.append("<BS>"); break;
                    case '\u0009': sb.append("<HT>"); break;

                    case '\u000B': sb.append("<VT>"); break;
                    case '\u000C': sb.append("<FF>"); break;

                    case '\u000E': sb.append("<SO>"); break;
                    case '\u000F': sb.append("<SI>"); break;
                    case '\u0010': sb.append("<DLE>"); break;
                    case '\u0011': sb.append("<DC1>"); break;
                    case '\u0012': sb.append("<DC2>"); break;
                    case '\u0013': sb.append("<DC3>"); break;
                    case '\u0014': sb.append("<DC4>"); break;
                    case '\u0015': sb.append("<NAK>"); break;
                    case '\u0016': sb.append("<SYN>"); break;
                    case '\u0017': sb.append("<ETB>"); break;
                    case '\u0018': sb.append("<CAN>"); break;
                    case '\u0019': sb.append("<EM>"); break;
                    case '\u001A': sb.append("<SUB>"); break;
                    case '\u001B': sb.append("<ESC>"); break;
                    case '\u001C': sb.append("<FS>"); break;
                    case '\u001D': sb.append("<GS>"); break;
                    case '\u001E': sb.append("<RS>"); break;
                    case '\u001F': sb.append("<US>"); break;
                    case '\u007F': sb.append("<DEL>"); break;
                    default:
                        if (c > '\u007F')
                            sb.append(String.format("\\u{0:X4}", c));
                        else
                            sb.append(c);
                        break;
                }
            }
            return sb.toString();
        }

        /// <summary>
        /// ASCII bytes to hexadecimal string
        /// </summary>
        /// <param name="b">ASCII bytes</param>
        /// <returns>hexadecimal string</returns>
        protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
        public static String bytesToHexString(@NonNull byte[] b) {
            char[] hexChars = new char[b.length * 2];
            int v;

            for (int j = 0; j < b.length; j++) {
                v = b[j] & 0xFF;
                hexChars[j * 2] = hexArray[v >> 4];
                hexChars[j * 2 + 1] = hexArray[v & 0x0F];
            }
            return new String(hexChars);
        }

        /// <summary>
        /// ASCII byte to hexadecimal string
        /// </summary>
        /// <param name="b">ASCII byte</param>
        /// <returns>hexadecimal string</returns>
        public static String byteToHexString(byte b) {
            char[] hexChars = new char[2];
            int v = b & 0xFF;

            hexChars[0] = hexArray[v >> 4];
            hexChars[1] = hexArray[v & 0x0F];
            return new String(hexChars);
        }

        /// <summary>
        /// hexadecimal string to ASCII bytes
        /// </summary>
        /// <param name="hex">hexadecimal string</param>
        /// <returns>ASCII bytes</returns>
        public static byte[] hexStringToBytes(String hex) throws ArithmeticException {
            byte[] b = null;

            if (hex.length() % 2 != 0)
                throw new ArithmeticException("字串長度非2倍數");
            byte[] data = new byte[hex.length()/2];

            for(int i = 0; i < hex.length(); i+=2) {
                data[i/2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4) + Character.digit(hex.charAt(i+1), 16));
            }
            return data;
        }

        /// <summary>
        /// hexadecimal string to ASCII byte
        /// </summary>
        /// <param name="s">hexadecimal string</param>
        /// <returns>ASCII byte</returns>
        public static byte hexStringToByte(String s) throws ArrayIndexOutOfBoundsException {
            int len = s.length();
            if (len == 2) {
                return Byte.parseByte(s, 16);
            }
            else
                throw new  ArrayIndexOutOfBoundsException("字串長度非等於2");
        }

        /// <summary>
        ///
        /// </summary>
        /// <param name="s"></param>
        /// <returns></returns>
        public static int hexStringToInt(String s) {
            return Integer.parseInt(s, 16);
        }

        /// <summary>
        ///
        /// </summary>
        /// <param name="s"></param>
        /// <returns></returns>
        public static byte[] stringToBytes(@NonNull String s) {
            return s.getBytes(StandardCharsets.US_ASCII);
        }

        /// <summary>
        /// string to hexadecimal string
        /// </summary>
        /// <param name="str"></param>
        /// <returns></returns>
        public static String stringToHexString(String s) {
            byte[] bs = s.getBytes(StandardCharsets.US_ASCII);
            return bytesToHexString(bs);
        }

        /// <summary>
        /// remove \r, \n
        /// </summary>
        /// <param name="s"></param>
        /// <returns></returns>
        public static String removeCRLF(String s) {
            String str = s.replace("\r", "").replace("\n", "");
            return str;
        }

        public static String removeCRLFandTarget(String s, String target) {
            String str = s.replace("\r", "").replace("\n", "").replace(target, "");
            return str;
        }

        /// <summary>
        /// show \r, \n to &lt;CR>, &lt;LF>
        /// </summary>
        /// <param name="str"></param>
        /// <returns></returns>
        public static String showCRLF(String s) {
            String str = s.replace("\r", "<CR>").replace("\n", "<LF>");
            return str;
        }
    }

    private ReaderService command_(byte b) {
        this.mBuilder.append(Format.makesUpZero(Format.byteToHexString(b), 2));
        return this;
    }

    private ReaderService command_(byte[] bs) {
        this.mBuilder.append(Format.bytesToHexString(bs));
        return this;
    }

    private ReaderService command_(String str) {
        this.mBuilder.append(Format.stringToHexString(str.toUpperCase()));
        return this;
    }

    private ReaderService bank_(String str) {
        this.mBuilder.append(Format.stringToHexString(str.toUpperCase()));
        return this;
    }

    private ReaderService address_(String str) {
        this.mBuilder.append(Format.stringToHexString(str.toUpperCase()));
        return this;
    }

    private ReaderService length_(String str) {
        this.mBuilder.append(Format.stringToHexString(str.toUpperCase()));
        return this;
    }

    private ReaderService data_(byte b) {
        this.mBuilder.append(Format.byteToHexString(b));
        return this;
    }

    private ReaderService data_(String str) {
        this.mBuilder.append(Format.stringToHexString(str.toUpperCase()));
        return this;
    }

    /**
     * @param str password length are eight, otherwise will ignore or zero to fill an array.
     * */
    private ReaderService password_(String str) {
        this.mBuilder.append(Format.stringToHexString(Format.makesUpZero(str.toUpperCase(), 8)));
        return this;
    }

    /**
     * @param str recommissioning length is one, otherwise will ignore
     * */
    private ReaderService recom_(String str) {
        this.mBuilder.append(Format.stringToHexString(Format.makesUpZero(str.toUpperCase(), 1)));
        return this;
    }

    /**
     * @param str mask length are three, otherwise will ignore or zero to fill an array.
     * */
    private ReaderService mask_(String str) {
        this.mBuilder.append(Format.stringToHexString(Format.makesUpZero(str.toUpperCase(), 3)));
        return this;
    }

    /**
     * @param str action length are three, otherwise will ignore or zero to fill an array.
     * */
    private ReaderService action_(String str) {
        this.mBuilder.append(Format.stringToHexString(Format.makesUpZero(str.toUpperCase(), 3)));
        return this;
    }

    private ReaderService comma_() {
        this.mBuilder.append(Format.stringToHexString(","));
        return this;
    }


    private byte[] commit() throws IllegalArgumentException {
        if (this.mBuilder == null)
            throw new IllegalArgumentException("StringBuilder is null");

        this.mBuilder
                .insert(0, Format.makesUpZero(Format.byteToHexString((byte)0x0A), 2))
                .append(Format.makesUpZero(Format.byteToHexString((byte)0x0D), 2));

        String str = this.mBuilder.toString();
        this.mBuilder.setLength(0);

        return Format.hexStringToBytes(str);
    }





    /**
     * Appends the V command that Reader firmware version
     * @return the command value of Reader firmware version
     */
    public byte[] V() {
        return this.command_((byte)0x56).commit();
    }

    /**
     * Appends the S command that Reader ID
     * @return the command value of Reader ID
     */
    public byte[] S() {
        return this.command_((byte)0x53).commit();
    }

    /**
     * Appends the Q command that read Tags EPC{PC,EPC,CRC16} data
     */
    public byte[] Q() {
        return this.command_((byte)0x51).commit();
    }

    /**
     * Appends the TID command that read Tags TID data
     * @return  the command value of TID
     */
    public byte[] TID() { return this.R("2", "0", "4");}

    /**
     * Appends the R command that read part or all of a Tags memory
     * @param bank memory bank(0 ~ 0x3)
     * @param address start address(0 ~ 0x3FFF)
     * @param length read word length(1 ~ 0x20)
     * @throws IllegalArgumentException when a method is invoked with
     * an argument which it can not reasonably deal with.
     * */
    public byte[] R(@NonNull String bank, @NonNull String address, @NonNull String length) throws IllegalArgumentException {
        if (Integer.parseInt(bank,16) < 0 && Integer.parseInt(bank,16) > 0x3)
            throw new IllegalArgumentException("bank parameter is out of range: 0 ~ 0x3");
        if (Integer.parseInt(address,16) < 0 && Integer.parseInt(address,16) > 0x3FFF)
            throw new IllegalArgumentException("length parameter is out of range: 0 ~ 0x3FFF");
        if (Integer.parseInt(length,16) < 1 && Integer.parseInt(length,16) > 0x20)
            throw new IllegalArgumentException("length parameter is out of range: 1 ~ 0x20");
        return this.command_((byte)0x52).bank_(bank).comma_().address_(address).comma_().length_(length).commit();
    }

    /**
     * Appends the W command that write data to a Tags memory
     * @param bank memory bank(0 ~ 0x3)
     * @param address start address(0 ~ 0x3FFF)
     * @param length read word length(1 ~ 0x20)
     * @param data data(hex)
     * @throws IllegalArgumentException when a method is invoked with
     * an argument which it can not reasonably deal with.
     * */
    public byte[] W(@NonNull String bank, @NonNull String address, @NonNull String length, @NonNull String data) throws IllegalArgumentException{
        if (Integer.parseInt(bank,16) < 0 && Integer.parseInt(bank,16) > 0x3)
            throw new IllegalArgumentException("bank parameter is out of range: 0 ~ 0x3");
        if (Integer.parseInt(address,16) < 0 && Integer.parseInt(address,16) > 0x3FFF)
            throw new IllegalArgumentException("length parameter is out of range: 0 ~ 0x3FFF");
        if (Integer.parseInt(length,16) < 1 && Integer.parseInt(length,16) > 0x20)
            throw new IllegalArgumentException("length is out of range: 1 ~ 0x20");
        int lLength = data.length();
        int lWordsLength = Integer.parseInt(length, 16);
        if (lWordsLength * 4 != lLength) {
            if (lWordsLength * 4 > lLength)
                throw new IllegalArgumentException(
                        new String("Length and data field is not match, data field must be fill " +
                                (lWordsLength * 4 - lLength) + " char."));
            else
                throw new IllegalArgumentException(
                        new String("Length and data field is not match, data field must be remove " +
                                (lLength - lWordsLength * 4) + " char."));
        }
        return this.command_((byte)0x57).bank_(bank).comma_()
                .address_(address).comma_()
                .length_(length).comma_()
                .data_(data).commit();
    }

    /**
     * Appends the K command that permanently disable a Tag
     * @param password kill password(0x0 ~ 0xFFFFFFFF)
     * @param recom recommissioning(0 ~ 7)
     * */
    public byte[] K(@NonNull String password, @NonNull String recom)
    {
        return this.command_((byte)0x4B).password_(password).comma_().recom_(recom).commit();
    }

    /**
     * Appends the L command that Tags memory lock operation
     * @param mask lock mask(0 ~ 0x3FF)
     * @param action lock action(0 ~ 0x3FF)
     * */
    public byte[] L(@NonNull String mask, @NonNull String action) {
        return this.command_((byte)0x4C).mask_(mask).comma_().action_(action).commit();
    }

    /**
     * <font face="Consolas" size="3" color="#ff9933"><i><b>Appends the P command that set access password for R,W,L command, one-time command</b></i></font>
     * @param password <font face="Consolas" color="#003366">access password<font color="#800000">(0x0 ~ 0xFFFFFFFF)</font></font>
     * */
    public byte[] P(@NonNull String password) {
        return this.command_((byte)0x50).password_(password).commit();
    }

    /**
     * <font face="Consolas" size="3" color="#ff9933"><i><b>Appends the U command that multi-Tags read EPC data</b></i></font>
     * */
    public byte[] U() {
        return this.command_((byte)0x55).commit();
    }

    /**
     * <font face="Consolas" size="3" color="#ff9933"><i><b>Appends the U command that multi-Tags read EPC data with slot Q</b></i></font>
     * @param slotQ <font face="Consolas" color="#003366">slot-count parameter Q<font color="#800000">(1 ~ 0xA)</font></font>
     * @throws IllegalArgumentException when a method is invoked with
     * an argument which it can not reasonably deal with.
     * */
    public byte[] U(@NonNull String slotQ) throws IllegalArgumentException {
        if (Integer.parseInt(slotQ,16) < 1 && Integer.parseInt(slotQ,16) > 0x0A)
            throw new IllegalArgumentException("slotQ is out of range: 1 ~ 10");
        return this.command_((byte)0x55).command_(slotQ).commit();
    }

    /**
     * <font face="Consolas" size="3" color="#ff9933"><i><b>Multi read EPC command with slot Q and read command</b></i></font>
     * @param slotQ <font face="Consolas" color="#003366">slot-count parameter Q<font color="#800000">(1 ~ 0xA)</font></font>
     * @param bank  <font face="Consolas" color="#003366">memory bank<font color="#800000">(0 ~ 0x3)</font></font>
     * @param address <font face="Consolas" color="#003366">start address<font color="#800000">(0 ~ 0x3FFF)</font></font>
     * @param length <font face="Consolas" color="#003366">read word length<font color="#800000">(1 ~ 0x20)</font></font>
     * @return the UR command bytes value
     * @throws IllegalArgumentException when a method is invoked with
     * an argument which it can not reasonably deal with.
     * */
    public byte[] UR(@Nullable String slotQ, @NonNull String bank, @NonNull String address, @NonNull String length) throws IllegalArgumentException {
        if (Integer.parseInt(bank,16) < 0 && Integer.parseInt(bank,16) > 0x3)
            throw new IllegalArgumentException("bank parameter is out of range: 0 ~ 0x3");
        if (Integer.parseInt(address,16) < 0 && Integer.parseInt(address,16) > 0x3FFF)
            throw new IllegalArgumentException("length parameter is out of range: 0 ~ 0x3FFF");
        if (Integer.parseInt(length,16) < 1 && Integer.parseInt(length,16) > 0x20)
            throw new IllegalArgumentException("length parameter is out of range: 1 ~ 0x20");
        if (slotQ != null) {
            if (Integer.parseInt(slotQ,16) < 1 && Integer.parseInt(slotQ,16) > 0x0A)
                throw new IllegalArgumentException("slotQ is out of range: 1 ~ 10");
            return this.command_((byte) 0x55).command_(slotQ).comma_()
                    .command_((byte) 0x52).bank_(bank).comma_()
                    .address_(address).comma_()
                    .length_(length).commit();
        }
        else
            return this.command_((byte)0x55).comma_().
                    command_((byte)0x52).bank_(bank).comma_()
                    .address_(address).comma_()
                    .length_(length).commit();
    }

    /**
     * <font face="Consolas" size="3" color="#ff9933"><i><b>Read EPC with read command</b></i></font>
     * @param bank  <font face="Consolas" color="#003366">memory bank<font color="#800000">(0 ~ 0x3)</font></font>
     * @param address <font face="Consolas" color="#003366">start address<font color="#800000">(0 ~ 0x3FFF)</font></font>
     * @param length <font face="Consolas" color="#003366">read word length<font color="#800000">(1 ~ 0x20)</font></font>
     * @return the QR command bytes value
     * @throws IllegalArgumentException when a method is invoked with
     * an argument which it can not reasonably deal with.
     * */
    public byte[] QR(@NonNull String bank, @NonNull String address, @NonNull String length) throws IllegalArgumentException {
        if (Integer.parseInt(bank,16) < 0 && Integer.parseInt(bank,16) > 0x3)
            throw new IllegalArgumentException("bank parameter is out of range: 0 ~ 0x3");
        if (Integer.parseInt(address,16) < 0 && Integer.parseInt(address,16) > 0x3FFF)
            throw new IllegalArgumentException("length parameter is out of range: 0 ~ 0x3FFF");
        if (Integer.parseInt(length,16) < 1 && Integer.parseInt(length,16) > 0x20)
            throw new IllegalArgumentException("length parameter is out of range: 1 ~ 0x20");
        return this.command_((byte)0x51).comma_().
                    command_((byte)0x52).bank_(bank).comma_()
                    .address_(address).comma_()
                    .length_(length).commit();
    }


    /**
     * <font face="Consolas" size="3" color="#ff9933"><i><b>Appends the T command that select match Tag</b></i></font>
     * @param bank memory bank<font color="#800000">(0 ~ 0x3)</font>
     * @param address start bit address<font color="#800000">(0 ~ 0x3FFF)</font>
     * @param length select bit length<font color="#800000">(1 ~ 0x60)</font>
     * @param data select bit mask data
     * @return the T command bytes value
     * @throws IllegalArgumentException when a method is invoked with
     * an argument which it can not reasonably deal with.
     * */
    public byte[] T(String bank, String address, String length, String data) throws IllegalArgumentException {
        if (Integer.parseInt(bank,16) < 0 && Integer.parseInt(bank,16) > 0x3)
            throw new IllegalArgumentException("bank parameter is out of range: 0 ~ 0x3");
        if (Integer.parseInt(address,16) < 0 && Integer.parseInt(address,16) > 0x3FFF)
            throw new IllegalArgumentException("address is out of range: 0 ~ 0x3FFF");
        if (Integer.parseInt(length,16) < 1 && Integer.parseInt(length,16) > 0x60)
            throw new IllegalArgumentException("length is out of range: 1 ~ 0x60");
        int lLength = data.length();
        int lBitsLength = Integer.parseInt(length, 16);
        int lMax = lLength * 4;
        int lMin = lLength * 4 - 3;

        if ((lBitsLength < lMin) || (lBitsLength > lMax)) {
            throw new IllegalArgumentException(
                    new String("Bit length and data field is not match, base on data field, "
                            + "the bit length value range is: "
                            + lMin + "~"
                            + lMax));
        }

        return this.command_((byte)0x54).bank_(bank).comma_()
                .address_(address).comma_()
                .length_(length).comma_()
                .data_(data).commit();
    }

    /**
     * <font face="Consolas" size="3" color="#ff9933"><i><b>Appends the N command that get RFID Reader power</b></i></font>
     * @return the get RFID power bytes value
     */
    public byte[] readPower() {
        return this.command_("N0,00").commit();
    }

    /**
     * <font face="Consolas" size="3" color="#ff9933"><i>Appends the N command that set RFID Reader power</i></font>
     * @param value the set RFID power bytes value<font color="#800000">(0x0 ~ 0x1B)</font>
     * @return the set RFID power bytes value
     */
    public byte[] setPower(String value) {
        return this.command_("N1,").data_(Format.makesUpZero(value.toUpperCase(), 2)).commit();
    }

    /**
     * <font face="Consolas" size="3" color="#ff9933"><i><b>Appends the N command that get regulation</b></i></font>
     * @return the get regulation bytes value
     */
    public byte[] readRegulation() {
        return this.command_("N4,00").commit();
    }

    /**
     * <font face="Consolas" size="3" color="#ff9933"><i><b>Appends the N command that set regulation</b></i></font>
     * @param value regulation area
     * @return the set regulation bytes value
     * */
    public byte[] setRegulation(@NonNull ReaderModule.Regulation value) {
        return this.command_("N5,").data_(value.toString()).commit();
    }

    /**
     * <font face="Consolas" size="3" color="#ff9933"><i><b>Appends the N command that get GPIO configuration</b></i></font>
     * @return the get GPIO configuration bytes value
     */
    public byte[] getGPIO() {
        return this.command_("N6,00").commit();
    }


    /**
     * <font face="Consolas" size="3" color="#ff9933"><i><b>Appends the N command that set GPIO configuration</b></i></font>
     * @param value mask and setting
     * @return the set GPIO configuration bytes value
     */
    public byte[] setGPIO(String value) {
        return this.command_("N7,").data_(Format.makesUpZero(value.toUpperCase(), 2)).commit();
    }


    /**
     * <font face="Consolas" size="3" color="#ff9933"><i><b>Appends the I command that get RFID Reader frequency</b></i></font>
     * @return the get RFID Reader frequency bytes value
     * */
    public byte[] readFrequencyChannel() {

        return this.command_("I4008702").commit();
    }


    /**
     * <font face="Consolas" size="3" color="#ff9933"><i><b>Appends the I command that get RFID Reader frequency offset</b></i></font>
     * @return the get RFID Reader frequency offset bytes value
     */
    public byte[] readFrequencyOffset() {

        return this.command_("I4008903").commit();
    }

    /**
     * <font face="Consolas" size="3" color="#ff9933"><i><b>Appends the I command that set RFID Reader frequency MSB</b></i></font>
     * @param value
     * @return
     */
    public byte[] setFrequencyAddrH(@NonNull String value) {

        return this.command_("I5008A").data_(value).commit();
    }

    /**
     * <font face="Consolas" size="3" color="#ff9933"><i><b>Appends the I command that set RFID Reader frequency LSB</b></i></font>
     * @param value
     * @return
     */
    public byte[] setFrequencyAddrL( @NonNull String value) { return this.command_("I5008B").data_(value).commit(); }

    /**
     * <font face="Consolas" size="3" color="#ff9933"><i><b>Appends the I command that set RFID Reader frequency</b></i></font>
     * @param value
     * @return
     */
    public byte[] setFrequency(@NonNull String value) { return this.command_("I60089").data_(value).commit(); }



    public byte[] raw(@NonNull String data) {

        return this.data_(data).commit();
    }

    /**
     * command that header byte is AA
     * */
    public byte[] AA(String type, String command, String address, String data) {

        this.mBuilder.append(Format.makesUpZero(type, 2))
                .append(Format.makesUpZero(command, 2))
                .append(Format.makesUpZero(address, 4))
                .append(Format.makesUpZero(data, 2));

        byte length = (byte)((this.mBuilder.length() + 2) / 2);
        this.mBuilder.insert(0, Format.makesUpZero(Format.byteToHexString(length), 2));

        byte crcData = Format.crc8(Format.hexStringToBytes(this.mBuilder.toString()));
        this.mBuilder.append(Format.makesUpZero(Format.byteToHexString(crcData), 2));
        this.mBuilder.insert(0, "AA");

        String callback = this.mBuilder.toString();
        if (this.mBuilder != null) this.mBuilder.setLength(0);

        return Format.hexStringToBytes(callback);

    }


    public byte[] AA(String data) {
        String callback;

        this.mBuilder.append(data);
        byte b = (byte)((this.mBuilder.length() + 2) / 2);
        this.mBuilder.insert(0, Format.makesUpZero(Format.byteToHexString(b), 2));
        b = Format.crc8(Format.hexStringToBytes(this.mBuilder.toString()));
        this.mBuilder.append(Format.makesUpZero(Format.byteToHexString(b), 2));
        this.mBuilder.insert(0, "AA");
        callback = this.mBuilder.toString();
        if (this.mBuilder != null) this.mBuilder.setLength(0);

        return Format.hexStringToBytes(callback);
    }
}
