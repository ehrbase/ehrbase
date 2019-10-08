package org.ehrbase.dao.access.util;

public class StoredQueryQualifiedName {

    String name;

    public StoredQueryQualifiedName(String name) {

        if (!name.contains("::"))
            throw new IllegalArgumentException("Qualified name is not valid (https://specifications.openehr.org/releases/SM/latest/openehr_platform.html#_query_package):"+name);

        this.name = name;
    }

    public StoredQueryQualifiedName(String reverseDomainName, String semanticId, String semVer) {
        this.name = reverseDomainName+"::"+semanticId+"/"+semVer;
    }

    public String reverseDomainName(){
        return name.split("::")[0];
    }

    public String semanticId(){
        return (name.split("::")[1]).split("/")[0];
    }

    public String semVer(){
        String semVer = null;

        if (name.contains("/"))
            semVer = name.split("/")[1];

        return semVer;
    }

    public boolean isSetSemVer(){
       return name.contains("/");
    }

    public String toString(){
        return name;
    }
}
