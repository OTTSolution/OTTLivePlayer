package com.xugaoxiang.live_vtm.bean;

import java.util.List;

public class LiveBean {

    /**
     * type : null
     * num : 1
     * name : 电影1台
     * url : http://202.158.177.67:8081/live/livestream1/index.m3u8
     * en_name : en_name
     */

    private List<DataBean> data;

    public List<DataBean> getData() {
        return data;
    }

    public void setData(List<DataBean> data) {
        this.data = data;
    }

    public static class DataBean {
        private Object type;
        private String num;
        private String name;
        private String url;
        private String en_name;

        public Object getType() {
            return type;
        }

        public void setType(Object type) {
            this.type = type;
        }

        public String getNum() {
            return num;
        }

        public void setNum(String num) {
            this.num = num;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getEn_name() {
            return en_name;
        }

        public void setEn_name(String en_name) {
            this.en_name = en_name;
        }
    }
}
