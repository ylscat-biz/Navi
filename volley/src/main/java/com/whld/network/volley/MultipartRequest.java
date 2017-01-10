package com.whld.network.volley;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Map;

/**
 * Created at 2016/4/7.
 *
 * @author YinLanShan
 */
public class MultipartRequest extends JsonRequest {
    private static final String BOUNDARY = "FsBoundaryZmFuZ3N0YXIubmV0";

    private ArrayList<FileEntity> mFiles = new ArrayList<FileEntity>(1);

    private long mCompletedBytes;

    public MultipartRequest(String url, Map<String, String> params, ResponseHandler handler) {
        super(Method.POST, url, params, handler);
    }

    public void addFile(String name, String contentType, File file) {
        mFiles.add(new FileEntity(name, contentType, file));
    }

    public void addFile(String name, String contentType, String fileName, File file) {
        mFiles.add(new FileEntity(name, contentType, fileName, file));
    }

    public ArrayList<FileEntity> getFiles() {
        return mFiles;
    }

    public long getCompletedBytes() {
        return mCompletedBytes;
    }

    @Override
    public void writeBody(OutputStream out) throws IOException {
        if(mParams != null)
            for(Map.Entry<String, String> entry : mParams.entrySet()) {
                writeParam(out, entry.getKey(), entry.getValue());
            }
        mCompletedBytes = 0;
        for(FileEntity entity : mFiles) {
            writeFile(out, entity);
        }
        writeEnd(out);
    }

    private void writeParam(OutputStream out, String name, String value)
            throws IOException{
        out.write("--".getBytes());
        out.write(BOUNDARY.getBytes());
        out.write("\r\n".getBytes());
        out.write("Content-Disposition: form-data; name=\"".getBytes());
        if(name != null)
            out.write(name.getBytes());
        out.write("\"\r\n\r\n".getBytes());
        if(value != null)
            out.write(value.getBytes());
        out.write("\r\n".getBytes());
    }

    private void writeFile(OutputStream out, FileEntity entity)
            throws IOException{
        File file = entity.file;
        if(file == null || !file.exists())
            return;
        out.write("--".getBytes());
        out.write(BOUNDARY.getBytes());
        out.write("\r\n".getBytes());
        out.write("Content-Disposition: form-data; name=\"".getBytes());
        out.write(entity.name.getBytes());
        out.write("\"; filename=\"".getBytes());
        if(entity.fileName != null && entity.fileName.length() > 0)
            out.write(entity.fileName.getBytes());
        else
            out.write(file.getName().getBytes());
        out.write("\"\r\n".getBytes());
        out.write("Content-Type: ".getBytes());
        out.write(entity.contentType.getBytes());
        out.write("\r\n\r\n".getBytes());
        FileInputStream fis = new FileInputStream(file);
        final int BUF_SIZE = 4096;
        byte[] buf = new byte[BUF_SIZE];
        for(int len = fis.read(buf); len != -1; len = fis.read(buf)) {
            out.write(buf, 0, len);
            mCompletedBytes += len;
        }
        out.write("\r\n".getBytes());
    }

    private void writeEnd(OutputStream out) throws IOException {
        byte[] sym = "--".getBytes();
        out.write(sym);
        out.write(BOUNDARY.getBytes());
        out.write(sym);
    }

    @Override
    public String getBodyContentType() {
        return "multipart/form-data; boundary=" + BOUNDARY;
    }

    public static class FileEntity {
        public String name, contentType, fileName;
        public File file;

        public FileEntity(String name, String contentType, File file) {
            this.name = name;
            this.contentType = contentType;
            this.file = file;
        }

        public FileEntity(String name, String contentType, String fileName, File file) {
            this.name = name;
            this.contentType = contentType;
            this.fileName = fileName;
            this.file = file;
        }
    }
}
