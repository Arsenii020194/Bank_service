package controllers.inetshop;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.openhtmltopdf.DOMBuilder;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import dto.inetshop.BillDTO;
import entities.Bill;
import entities.UserData;
import facades.UserDataFacade;
import facades.inetshop.BillsFacade;
import html.UserBillToHtmlConverter;
import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import play.mvc.Controller;
import play.mvc.Result;

import javax.inject.Inject;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class BillGenerationController extends Controller {

    @Inject
    private UserDataFacade userDataFacade;

    @Inject
    private BillsFacade billsFacade;

    public Result generatePdf() throws IOException {

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


        builder.withW3cDocument(doc1, "");
        builder.toStream(fileStream);
        try {
            builder.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
        response().setHeader("Content-Type", "application/pdf");
        saveBill(billDTO, fileStream.toByteArray());
        return ok(fileStream.toByteArray());
    }

    public Result view() {
        return ok(views.html.bill_view.render(billsFacade.getAllBills(userDataFacade.getUser())));
    }

    public Result download(Integer id) {
        return ok(billsFacade.getFileById(id));
    }


    private void saveBill(BillDTO billDTO, byte[] file) {
        Bill bill = new Bill();
        Timestamp nowDate = Timestamp.valueOf(LocalDate.now().atStartOfDay());
        Timestamp activeFor = Timestamp.valueOf(LocalDate.now().plusDays(userDataFacade.getUserData().getBillProlongation()).atStartOfDay());
        bill.setDate(nowDate);
        bill.setActiveFor(activeFor);
        bill.setCustomer(billDTO.getCustomer());
        bill.setNum(Integer.valueOf(billDTO.getBillNumber()));
        bill.setReciever(userDataFacade.getUser());
        bill.setNum(Integer.valueOf(billDTO.getNumOrder()));
        bill.setUslugs(billDTO.getUslugs().stream().map(f -> new Gson().toJson(f)).collect(Collectors.toList()));
        bill.setFile(file);
        bill.save();
    }
}
