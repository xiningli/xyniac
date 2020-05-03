package com.xyniac;

public class JavaPerson {
    String name;
    int age;

    @Override
    public String toString() {
        return "{\"JavaPerson\":{"
                + "                        \"name\":\"" + name + "\""
                + ",                         \"age\":\"" + age + "\""
                + "}}";
    }
}
