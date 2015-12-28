package com.ssl.cltracker.service;

public interface CLTrackerConstants {

    public static final String KEY_FILE_REVISION = "rev0";
    public static final String KEY_FILE_CHANGELIST = "change0";
    public static final String KEY_FILE_FULLPATH = "depotFile";
    public static final String KEY_FILE_ACTION = "action0";

    public static final String KEY_DESCENDANT_FILE_FULLPATH = "file0,";
    public static final String KEY_DESCENDANT_FILE_ACTION = "how0,";
    public static final String KEY_DESCENDANT_FILE_REVISION_START = "srev0,";   //returns in the form of #[number] or #none
    public static final String KEY_DESCENDANT_FILE_REVISION_END = "erev0,";     //returns in the form of #[number] or #none

    public static final String KEY_TRIVIAL_CLIENT = "client0";
    public static final String KEY_TRIVIAL_DIGEST = "digest0";
    public static final String KEY_TRIVIAL_TIME = "time0";
    public static final String KEY_TRIVIAL_USER = "user0";
    public static final String KEY_TRIVIAL_DESC = "desc0";
    public static final String KEY_TRIVIAL_TYPE = "type0";
    public static final String KEY_TRIVIAL_FILESIZE= "fileSize0";

    public static final int MAP_NON_DESCENDANT_KEYS = 11;
    public static final int MAP_DESCENDANT_KEYSET = 4;

    //Keys for p4 integrated keyword
    public static final String KEY_INTEGRATED_FROMFILE = "fromFile";
    public static final String KEY_INTEGRATED_HOW = "how";

    public static final String KEY_INTEGRATED_END_FROM_REVISION = "endFromRev"; //returns in the form of #[number] or #none
    public static final String KEY_INTEGRATED_START_FROM_REVISION = "startFromRev"; //returns in the form of #[number] or #none

    public static final String KEY_INTEGRATED_START_TO_REVISION = "startToRev"; //returns in the form of #[number] or #none
    public static final String KEY_INTEGRATED_END_TO_REVISION = "endToRev"; //returns in the form of #[number] or #none

    //TODO encrpyt the ID and password & make it outside.
    public static final String P4_ID = "sangku1.kang";
    public static final String P4_PASSWORD = "";

    public static final String P4_1716_ADDRESS = "p4java://105.59.102.30:1716";

    public static final String TABLE_DEV_TEAM3_ALIAS = "DEV_TEAM3";
    public static final String TABLE_DEV_SETTINGS_ALIAS = "DEV_Settings";
    public static final String TABLE_DEV_CALL_ALIAS = "DEV_CALL";
    public static final String TABLE_NILE_ALIAS = "NILE";
    public static final String TABLE_FLUMEN_GPRIME_ALIAS = "FLUMEN_GPRIME";
    public static final String TABLE_FLUMEN_TABS2_ALIAS = "FLUMEN_TABS2";
    public static final String TABLE_FLUMEN_THETIS_ALIAS = "FLUMEN_THETIS";
    public static final String TABLE_FLUMEN_NOBLEZEN_ALIAS = "FLUMEN_NOBLEZEN";
    public static final String TABLE_FLUMEN_ZERO_ALIAS = "FLUMEN_ZERO";
    public static final String TABLE_REL_GPRIME_ALIAS = "REL_GPRIME";
    public static final String TABLE_REL_THETIS_N910P_ALIAS = "REL_THETIS_N910P";
    public static final String TABLE_REL_THETIS_N915P_ALIAS = "REL_THETIS_N915P";
    public static final String TABLE_REL_NOBLEZEN_ALIAS = "REL_NOBLEZEN";
    public static final String TABLE_REL_ZERO_ALIAS = "REL_ZERO";
    public static final String TABLE_DEV_TEAM3_FULLNAME = "//DEV/Application/3rdPartyApp/";
    public static final String TABLE_DEV_SETTINGS_FULLNAME = "//DEV/Application/Settings/";
    public static final String TABLE_DEV_CALL_FULLNAME = "//DEV/Application/Call/";
    public static final String TABLE_NILE_FULLNAME = "//NILE/";
    public static final String TABLE_FLUMEN_GPRIME_FULLNAME = "//PROD_NILE/GPRIMELTE_NA/FLUMEN/";
    public static final String TABLE_FLUMEN_TABS2_FULLNAME = "//PROD_NILE/TAB_S2_NA/FLUMEN/";
    public static final String TABLE_FLUMEN_THETIS_FULLNAME = "//PROD_NILE/THETIS_51MR/FLUMEN/";
    public static final String TABLE_FLUMEN_NOBLEZEN_FULLNAME = "//PROD_NILE/NOBLE_ZERO2/FLUMEN/";
    public static final String TABLE_FLUMEN_ZERO_FULLNAME = "//PROD_NILE/ZERO_51MR/FLUMEN/";
    public static final String TABLE_REL_GPRIME_FULLNAME = "//PROD_NILE/GPRIMELTE_NA/SM-G530P_NA_SPR/";
    public static final String TABLE_REL_THETIS_N910P_FULLNAME = "//PROD_NILE/THETIS_51MR/SM-N910P_NA_LL_SPR/";
    public static final String TABLE_REL_THETIS_N915P_FULLNAME = "//PROD_NILE/THETIS_51MR/SM-N915P_NA_LL_SPR/";
    public static final String TABLE_REL_NOBLEZEN_FULLNAME = "//PROD_NILE/NOBLE_ZERO2/SM-N920_G928_NA_ALL/";
    public static final String TABLE_REL_ZERO_FULLNAME = "//PROD_NILE/ZERO_51MR/SM-G920_G925_NA_ALL_PMR/";
}
