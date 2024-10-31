# Release to Maven Central
To make `mvn deploy` work, your settings.xml should look as follows:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="https://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="https://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">
    <servers>
        <server>
            <id>central</id>
            <username>see https://central.sonatype.com/account</username>
            <password>see https://central.sonatype.com/account</password>
        </server>
    </servers>

    <profiles>
        <profile>
            <id>ossrh</id>
            <activation>
                <activeByDefault>true</activeByDefault>
            </activation>
            <properties>
                <gpg.executable>gpg</gpg.executable>
                <gpg.keyname>see gpg --list-keys and gpg --keyserver keyserver.ubuntu.com --send-keys [KEY_NAME]</gpg.keyname>
                <gpg.passphrase>see gpg --gen-key</gpg.passphrase>
            </properties>
        </profile>
    </profiles>
</settings>
```
