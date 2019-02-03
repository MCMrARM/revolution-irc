package io.mrarm.irc.chat.preview.cache;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "link_previews")
public class LinkPreviewInfo {

    public static int TYPE_WEBSITE = 0;
    public static int TYPE_IMAGE = 1;

    @PrimaryKey
    @ColumnInfo(name = "url")
    @NonNull
    private String mUrl;

    @ColumnInfo(name = "type")
    private int mType;

    @ColumnInfo(name = "title")
    private String mTitle;

    @ColumnInfo(name = "desc")
    private String mDescription;

    @ColumnInfo(name = "image")
    private String mImageUrl;

    private transient Bitmap mImage;

    public LinkPreviewInfo() {
    }

    private LinkPreviewInfo(@NonNull String url, int type, String title, String description,
                            String imageUrl, Bitmap image) {
        mUrl = url;
        mType = type;
        mTitle = title;
        mDescription = description;
        mImageUrl = imageUrl;
        mImage = image;
    }

    public String getUrl() {
        return mUrl;
    }

    public void setUrl(String mUrl) {
        this.mUrl = mUrl;
    }

    public int getType() {
        return mType;
    }

    public void setType(int mType) {
        this.mType = mType;
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String mTitle) {
        this.mTitle = mTitle;
    }

    public String getDescription() {
        return mDescription;
    }

    public void setDescription(String mDescription) {
        this.mDescription = mDescription;
    }

    public String getImageUrl() {
        return mImageUrl;
    }

    public void setImageUrl(String mImageUrl) {
        this.mImageUrl = mImageUrl;
    }

    public Bitmap getImage() {
        return mImage;
    }

    public void setImage(Bitmap mImage) {
        this.mImage = mImage;
    }

    public static LinkPreviewInfo fromWebsite(String url, String title, String description,
                                              String imageUrl, Bitmap image) {
        return new LinkPreviewInfo(url, TYPE_WEBSITE, title, description, imageUrl, image);
    }

    public static LinkPreviewInfo fromImage(String url, Bitmap image) {
        return new LinkPreviewInfo(url, TYPE_IMAGE, null, null, url, image);
    }

}
