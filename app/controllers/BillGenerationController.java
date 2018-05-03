package controllers;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.openhtmltopdf.DOMBuilder;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import dto.BillDTO;
import entities.UserData;
import facades.UserDataFacade;
import html.UserBillToHtmlConverter;
import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import play.mvc.Controller;
import play.mvc.Result;

import javax.inject.Inject;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

public class BillGenerationController extends Controller {

    @Inject
    private UserDataFacade userDataFacade;

    public Result generatePdf() throws TransformerException, IOException {

        String textBill = request().body().asText();
        Type billDTOType = new TypeToken<BillDTO>() {
        }.getType();
        BillDTO billDTO = new Gson().fromJson(textBill, billDTOType);
        UserData userData = userDataFacade.getUserData();
        UserBillToHtmlConverter userBillToHtmlConverter = new UserBillToHtmlConverter(billDTO, userData);
        PdfRendererBuilder builder = new PdfRendererBuilder();
        ByteArrayOutputStream fileStream = new ByteArrayOutputStream();
        File styleFile = new File("public/stylesheets/print.css");
        List<String> lines = FileUtils.readLines(styleFile, "UTF-8");
        StringBuilder styleBuilder = new StringBuilder();
        lines.forEach(styleBuilder::append);

        Document doc = Jsoup.parse(
                "<style>" +
                        styleBuilder.toString() +
                        "</style>");

        doc.body().append(userBillToHtmlConverter.convert());

        org.w3c.dom.Document doc1 = DOMBuilder.jsoup2DOM(doc);

        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();

        transformer.transform(new DOMSource(doc1), new StreamResult(System.out));

        builder.withW3cDocument(doc1, "");
        builder.toStream(fileStream);
        try {
            builder.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
        response().setHeader("Content-Type", "application/pdf");
        return ok(fileStream.toByteArray());
    }
}
