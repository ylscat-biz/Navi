package lite.navi.network;

/**
 * @author ylscat
 *         Date: 2016-12-05 06:23
 */

public interface Urls {
    String SERVER = "http://map.whu.whldsoft.com";

    String CREATE = SERVER + "/app/user/create";
    String PROFILE = SERVER + "/app/user/profile";
    String GROUP_CREATE = SERVER + "/app/group/create";
    String GROUP_JOIN = SERVER + "/app/group/join";
    String GROUP_MEMBER = SERVER + "/app/group/groupMemberInfo";
    String GROUP_NEARBY = SERVER + "/app/group/nearby";
    String GROUP_QUIT = SERVER + "/app/group/quit";
}
