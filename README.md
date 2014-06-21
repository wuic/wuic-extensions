### Web UI Compressor extensions

The table below describes every extensions of the WUIC project. An extension always comes with its own
dependencies. What you have simply to do is to include the extension's maven dependency:

```
<dependency>
    <groupId>com.github.wuic.extensions</groupId>
    <artifactId>artifact-name</artifactId>
    <version>${wuic-extension.version}</version>
</dependency>
```

<table width=100% height=100%>
    <tr>
        <td>Artifact</td>
        <td>Description</td>
        <td>Component type</td>
    </tr>
        <td>aws-s3</td>
        <td>
            Store your statics in the cloud with AWS and manage it with this extension.
        </td>
        <td>
            DAO
        </td>
    </tr>
    <tr>
        <td>ftp</td>
        <td>
            Store your statics in any FTP server and manage it with this extension.
        </td>
        <td>
            DAO
        </td>
    </tr>
    <tr>
        <td>google-storage</td>
        <td>
            Store your statics in the cloud with Google storage and manage it with this extension.
        </td>
        <td>
            DAO
        </td>
    </tr>
    <tr>
        <td>ssh</td>
        <td>
            Store your statics in any linux system and manage it through SFTP with this extension.
        </td>
        <td>
            DAO
        </td>
    </tr>
    <tr>
        <td>Thymeleaf</td>
        <td>
            Write your application with thymeleaf and uses this processor to integrate WUIC features.
        </td>
        <td>
            Tag
        </td>
    </tr>
</table>
