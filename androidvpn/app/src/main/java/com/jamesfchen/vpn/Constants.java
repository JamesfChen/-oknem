package com.jamesfchen.vpn;

import java.util.HashSet;

public class Constants {
    public static final String TAG = "cjfvpn";
    public static HashSet<String> sInterceptIps = new HashSet<String>() {
        {
            /**
             netease
             */
            //443
//            add("45.254.48.1");
//            add("59.111.181.38");
            add("59.111.181.60");
            add("115.236.118.33");
            add("59.111.181.35");
//            add("203.76.217.244");
            add("115.236.121.1");
            add("193.112.159.225");
            //80
            add("106.11.93.16");
            add("59.111.181.155");
        }
    };
}
