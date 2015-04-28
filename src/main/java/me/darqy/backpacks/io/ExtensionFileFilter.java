package me.darqy.backpacks.io;

import java.io.File;
import java.io.FileFilter;

public class ExtensionFileFilter implements FileFilter {
    
    private final String ext;
    
    public ExtensionFileFilter(String extension) {
        this.ext = extension.toLowerCase();
    }
    
    @Override
    public boolean accept(File file) {
        return file.getName().toLowerCase().endsWith(ext);
    }
    
    public String stripExtension(String string) {
        return string.substring(0, string.length() - ext.length());
    }
    
}
