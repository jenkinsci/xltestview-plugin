package com.xebialabs.xlt.ci;

import hudson.util.ListBoxModel;

import java.util.Collections;
import java.util.Comparator;

public class JellyUtil {

    public static void sortListBoxModel(ListBoxModel items) {
        Collections.sort(items, new Comparator<ListBoxModel.Option>() {
            @Override
            public int compare(ListBoxModel.Option o1, ListBoxModel.Option o2) {
                if (o1.name == null) {
                    return -1;
                }
                return o1.name.compareTo(o2.name);
            }
        });
    }
}
