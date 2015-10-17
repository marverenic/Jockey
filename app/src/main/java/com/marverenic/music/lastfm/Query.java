package com.marverenic.music.lastfm;

import android.content.Context;

import com.marverenic.music.instances.Artist;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Scanner;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class Query {

    private static String getQuery(String method) {
        return "http://ws.audioscrobbler.com/2.0/?method=" + method + "&api_key=" + Config.API_KEY;
    }

    public static LArtist getArtist(Context context, Artist artist)
            throws IOException, ParserConfigurationException, SAXException {

        if (Cache.hasItem(context, artist.artistId)) {
            return Cache.getCachedArtist(context, artist.artistId);
        }

        String request = getQuery("artist.getInfo") + "&artist=" + artist.artistName;

        String data = getData(request);
        if (!data.equals("")) {
            Document xmlResult = parse(data);

            Node artistTag = xmlResult.getElementsByTagName("artist").item(0);
            LArtist result = buildArtist(artistTag);

            Cache.cacheArtist(context, artist.artistId, result);
            return result;
        } else {
            return null;
        }
    }

    private static LArtist buildArtist(Node artistNode) {
        final LArtist result = new LArtist();

        NodeList children = artistNode.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);

            switch (child.getNodeName()) {
                case "name":
                    result.name = child.getTextContent();
                    break;
                case "mbid":
                    result.mbid = child.getTextContent();
                    break;
                case "url":
                    result.url = child.getTextContent();
                    break;
                case "image":
                    String size = child.getAttributes().item(0).getNodeValue();
                    switch (size) {
                        case "small":
                            result.images.smallUrl = child.getTextContent();
                            break;
                        case "medium":
                            result.images.mediumUrl = child.getTextContent();
                            break;
                        case "large":
                            result.images.largeUrl = child.getTextContent();
                            break;
                        case "xlarge":
                            result.images.xlargeUrl = child.getTextContent();
                            break;
                        case "mega":
                            result.images.megaUrl = child.getTextContent();
                            break;
                    }
                    break;
                case "similar":
                    NodeList similarChildren = artistNode.getChildNodes();
                    ArrayList<LArtist> related = new ArrayList<>();
                    for (int j = 0; j < similarChildren.getLength(); j++) {
                        Node similar = similarChildren.item(j);
                        if (similar.getNodeName().equals("artist")) {
                            related.add(buildArtist(similar));
                        }
                    }
                    result.relatedArtists = related.toArray(new LArtist[related.size()]);
                    break;
                case "tags":
                    result.tags = buildTagList(child);
                    break;
                case "bio":
                    result.bio = buildBio(child);
                case "#text":
                default:
                    break;
            }
        }

        return result;
    }

    private static Tag[] buildTagList(Node tags) {
        NodeList tagsChildren = tags.getChildNodes();
        ArrayList<Tag> tagList = new ArrayList<>();

        for (int i = 0; i < tagsChildren.getLength(); i++) {

            Node child = tagsChildren.item(i);

            if (child.getNodeName().equals("tag")) {
                tagList.add(buildTag(child.getChildNodes()));
            }

        }

        return tagList.toArray(new Tag[tagList.size()]);
    }

    private static Tag buildTag(NodeList tagChildren) {
        Tag result = new Tag();

        for (int j = 0; j < tagChildren.getLength(); j++) {
            Node child = tagChildren.item(j);

            switch (child.getNodeName()) {
                case "name":
                    result.name = child.getTextContent();
                    break;
                case "url":
                    result.url = child.getTextContent();
                    break;
                case "#text":
                default:
                    break;
            }
        }
        return result;
    }

    private static Bio buildBio(Node bio) {
        final Bio result = new Bio();

        NodeList children = bio.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);

            switch (child.getNodeName()) {
                case "published":
                    result.date = child.getTextContent();
                    break;
                case "summary":
                    result.summary = child.getTextContent();
                    break;
                case "content":
                    result.content = child.getTextContent();
                    break;
                case "#text":
                default:
                    break;
            }
        }

        return result;
    }

    private static String getData(String url) {
        try {
            Scanner netScan = new Scanner(new URL(url).openStream(), "UTF-8").useDelimiter("\\A");
            String out = netScan.next();
            netScan.close();
            return out;
        } catch (IOException e) {
            return "";
        }
    }

    private static Document parse(String xml) throws ParserConfigurationException,
            SAXException, IOException {

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        InputSource is = new InputSource(new StringReader(xml));
        return builder.parse(is);
    }

}
