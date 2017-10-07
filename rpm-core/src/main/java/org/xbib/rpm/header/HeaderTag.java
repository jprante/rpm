package org.xbib.rpm.header;

/**
 *
 */
public enum HeaderTag implements EntryType {

    NAME(1000, STRING_ENTRY, String.class, "name"),
    VERSION(1001, STRING_ENTRY, String.class, "version"),
    RELEASE(1002, STRING_ENTRY, String.class, "release"),
    EPOCH(1003, INT32_ENTRY, Integer[].class, "epoch"),
    SUMMARY(1004, I18NSTRING_ENTRY, String.class, "summary"),
    DESCRIPTION(1005, I18NSTRING_ENTRY, String.class, "description"),
    BUILDTIME(1006, INT32_ENTRY, Integer[].class, "buildtime"),
    BUILDHOST(1007, STRING_ENTRY, String.class, "buildhost"),
    SIZE(1009, INT32_ENTRY, Integer[].class, "size"),
    DISTRIBUTION(1010, STRING_ENTRY, String.class, "distribution"),
    VENDOR(1011, STRING_ENTRY, String.class, "vendor"),
    LICENSE(1014, STRING_ENTRY, String.class, "license"),
    PACKAGER(1015, STRING_ENTRY, String.class, "packager"),
    GROUP(1016, I18NSTRING_ENTRY, String.class, "group"),
    CHANGELOG(1017, STRING_ARRAY_ENTRY, String[].class, "changelog"),
    URL(1020, STRING_ENTRY, String.class, "url"),
    OS(1021, STRING_ENTRY, String.class, "os"),
    ARCH(1022, STRING_ENTRY, String.class, "arch"),
    SOURCERPM(1044, STRING_ENTRY, String.class, "sourcerpm"),
    FILEVERIFYFLAGS(1045, INT32_ENTRY, Integer[].class, "fileverifyflags"),
    ARCHIVESIZE(1046, INT32_ENTRY, Integer[].class, "archivesize"),
    RPMVERSION(1064, STRING_ENTRY, String.class, "rpmversion"),
    CHANGELOGTIME(1080, INT32_ENTRY, Integer[].class, "changelogtime"),
    CHANGELOGNAME(1081, STRING_ARRAY_ENTRY, String[].class, "changelogname"),
    CHANGELOGTEXT(1082, STRING_ARRAY_ENTRY, String[].class, "changelogtext"),
    COOKIE(1094, STRING_ENTRY, String.class, "cookie"),
    OPTFLAGS(1122, STRING_ENTRY, String.class, "optflags"),
    PAYLOADFORMAT(1124, STRING_ENTRY, String.class, "payloadformat"),
    PAYLOADCOMPRESSOR(1125, STRING_ENTRY, String.class, "payloadcompressor"),
    PAYLOADFLAGS(1126, STRING_ENTRY, String.class, "payloadflags"),
    RHNPLATFORM(1131, STRING_ENTRY, String.class, "rhnplatform"),
    PLATFORM(1132, STRING_ENTRY, String.class, "platform"),
    FILECOLORS(1140, INT32_ENTRY, Integer[].class, "filecolors"),
    FILECLASS(1141, INT32_ENTRY, Integer[].class, "fileclass"),
    CLASSDICT(1142, STRING_ARRAY_ENTRY, String[].class, "classdict"),
    FILEDEPENDSX(1143, INT32_ENTRY, Integer[].class, "filedependsx"),
    FILEDEPENDSN(1144, INT32_ENTRY, Integer[].class, "filedependsn"),
    DEPENDSDICT(1145, INT32_ENTRY, Integer[].class, "dependsdict"),
    SOURCEPKGID(1146, BIN_ENTRY, Object.class, "sourcepkgid"),
    FILECONTEXTS(1147, STRING_ARRAY_ENTRY, String[].class, "filecontexts"),

    HEADERIMMUTABLE(63, BIN_ENTRY, Object.class, "headerimmutable"),
    HEADERI18NTABLE(100, STRING_ARRAY_ENTRY, String[].class, "headeri18ntable"),

    PREINSCRIPT(1023, STRING_ENTRY, String.class, "prein"),
    POSTINSCRIPT(1024, STRING_ENTRY, String.class, "postin"),
    PREUNSCRIPT(1025, STRING_ENTRY, String.class, "preun"),
    POSTUNSCRIPT(1026, STRING_ENTRY, String.class, "postun"),
    PREINPROG(1085, STRING_ENTRY, String.class, "preinprog"),
    POSTINPROG(1086, STRING_ENTRY, String.class, "postinprog"),
    PREUNPROG(1087, STRING_ENTRY, String.class, "preunprog"),
    POSTUNPROG(1088, STRING_ENTRY, String.class, "postunprog"),

    PRETRANSSCRIPT(1151, STRING_ENTRY, String.class, "pretrans"),
    POSTTRANSSCRIPT(1152, STRING_ENTRY, String.class, "posttrans"),
    PRETRANSPROG(1153, STRING_ENTRY, String.class, "pretransprog"),
    POSTTRANSPROG(1154, STRING_ENTRY, String.class, "pretransprog"),

    TRIGGERSCRIPTS(1065, STRING_ARRAY_ENTRY, String[].class, "triggerscripts"),
    TRIGGERNAME(1066, STRING_ARRAY_ENTRY, String[].class, "triggername"),
    TRIGGERVERSION(1067, STRING_ARRAY_ENTRY, String[].class, "triggerversion"),
    TRIGGERFLAGS(1068, INT32_ENTRY, Integer[].class, "triggerflags"),
    TRIGGERINDEX(1069, INT32_ENTRY, Integer[].class, "triggerindex"),
    TRIGGERSCRIPTPROG(1092, STRING_ARRAY_ENTRY, String[].class, "triggerscriptprog"),

    OLDFILENAMES(1027, STRING_ARRAY_ENTRY, String[].class, "oldfilenames"),
    FILESIZES(1028, INT32_ENTRY, Integer[].class, "filesizes"),
    FILEMODES(1030, INT16_ENTRY, short[].class, "filemodes"),
    FILERDEVS(1033, INT16_ENTRY, short[].class, "filerdevs"),
    FILEMTIMES(1034, INT32_ENTRY, Integer[].class, "filemtimes"),
    FILEDIGESTS(1035, STRING_ARRAY_ENTRY, String[].class, "filedigests"),
    FILELINKTOS(1036, STRING_ARRAY_ENTRY, String[].class, "filelinktos"),
    FILEFLAGS(1037, INT32_ENTRY, Integer[].class, "fileflags"),
    FILEUSERNAME(1039, STRING_ARRAY_ENTRY, String[].class, "fileusername"),
    FILEGROUPNAME(1040, STRING_ARRAY_ENTRY, String[].class, "filegroupname"),
    FILEDEVICES(1095, INT32_ENTRY, Integer[].class, "filedevices"),
    FILEINODES(1096, INT32_ENTRY, Integer[].class, "fileinodes"),
    FILELANGS(1097, STRING_ARRAY_ENTRY, String[].class, "filelangs"),
    PREFIXES(1098, STRING_ARRAY_ENTRY, String[].class, "prefixes"),
    DIRINDEXES(1116, INT32_ENTRY, Integer[].class, "dirindexes"),
    BASENAMES(1117, STRING_ARRAY_ENTRY, String[].class, "basenames"),
    DIRNAMES(1118, STRING_ARRAY_ENTRY, String[].class, "dirnames"),

    PROVIDENAME(1047, STRING_ARRAY_ENTRY, String[].class, "providename"),
    REQUIREFLAGS(1048, INT32_ENTRY, Integer[].class, "requireflags"),
    REQUIRENAME(1049, STRING_ARRAY_ENTRY, String[].class, "requirename"),
    REQUIREVERSION(1050, STRING_ARRAY_ENTRY, String[].class, "requireversion"),
    CONFLICTFLAGS(1053, INT32_ENTRY, Integer[].class, "conflictflags"),
    CONFLICTNAME(1054, STRING_ARRAY_ENTRY, String[].class, "conflictname"),
    CONFLICTVERSION(1055, STRING_ARRAY_ENTRY, String[].class, "conflictversion"),
    OBSOLETENAME(1090, STRING_ARRAY_ENTRY, String[].class, "obsoletename"),
    PROVIDEFLAGS(1112, INT32_ENTRY, Integer[].class, "provideflags"),
    PROVIDEVERSION(1113, STRING_ARRAY_ENTRY, String[].class, "provideversion"),
    OBSOLETEFLAGS(1114, INT32_ENTRY, Integer[].class, "obsoleteflags"),
    OBSOLETEVERSION(1115, STRING_ARRAY_ENTRY, String[].class, "obsoleteversion"),

    FILEDIGESTALGOS(1177, INT32_ENTRY, Integer[].class, "filedigestalgos"),

    /* private header tags */

    SIGNATURES(0x0000003e, INT32_ENTRY, Integer[].class, "_signatures"),
    IMMUTABLE(0x0000003f, INT32_ENTRY, Integer[].class, "_immutable");

    private int code;

    private int type;

    private Class<?> typeClass;

    private String name;

    HeaderTag(int code, int type, Class<?> typeClass, String name) {
        this.code = code;
        this.type = type;
        this.typeClass = typeClass;
        this.name = name;
    }

    public int getCode() {
        return code;
    }

    public int getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public Class<?> getTypeClass() {
        return typeClass;
    }
}
