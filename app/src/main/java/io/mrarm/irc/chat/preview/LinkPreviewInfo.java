package io.mrarm.irc.chat.preview;

import android.graphics.Bitmap;

public class LinkPreviewInfo {

    public static int TYPE_WEBPAGE = 0;
    public static int TYPE_IMAGE = 1;

    private String mUrl;
    private String mTitle;
    private String mDescription;
    private String mImageUrl;
    private Bitmap mImage;

    private LinkPreviewInfo(String url, String title, String description, String imageUrl,
                           Bitmap image) {
        mUrl = url;
        mTitle = title;
        mDescription = description;
        mImageUrl = imageUrl;
        mImage = image;
    }

    public String getTitle() {
        return mTitle;
    }

    public String getDescription() {
        return mDescription;
    }

    public String getImageUrl() {
        return mImageUrl;
    }

    public Bitmap getImage() {
        return mImage;
    }

    public static LinkPreviewInfo fromWebsite(String url, String title, String description,
                                              String imageUrl, Bitmap image) {
        return new LinkPreviewInfo(url, title, description, imageUrl, image);
    }

    public static LinkPreviewInfo fromImage(String url, Bitmap image) {
        return new LinkPreviewInfo(url, null, null, url, image);
    }

}
