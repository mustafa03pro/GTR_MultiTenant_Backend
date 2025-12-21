package com.example.multi_tanent.pos.service;

import com.example.multi_tanent.pos.entity.ProductVariant;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.MultiFormatWriter;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Qualifier;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class BarCodeService {

    private final FileStorageService fileStorageService;

    public BarCodeService(@Qualifier("posFileStorageService") FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    private static final int QR_CODE_WIDTH = 250;
    private static final int QR_CODE_HEIGHT = 250;

    /**
     * Generates a QR code image for the given text.
     *
     * @param text   The text to encode in the QR code.
     * @param width  The width of the QR code image.
     * @param height The height of the QR code image.
     * @return A byte array representing the QR code image in PNG format.
     * @throws WriterException If an error occurs during QR code generation.
     * @throws IOException     If an error occurs while writing the image to the
     *                         byte stream.
     */
    public byte[] generateBarcodeImage(String text, int width, int height) throws WriterException, IOException {
        MultiFormatWriter writer = new MultiFormatWriter();
        BitMatrix bitMatrix = writer.encode(text, BarcodeFormat.CODE_128, width, height);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "JPEG", outputStream);
        return outputStream.toByteArray();
    }

    /**
     * Generates a barcode for a product variant, saves it as an image file,
     * and returns the relative path to the saved image.
     *
     * @param variant The ProductVariant entity.
     * @return The relative path of the saved barcode image.
     * @throws WriterException if the content cannot be encoded.
     * @throws IOException     if an I/O error occurs.
     */
    public String generateAndSaveProductVariantBarcode(ProductVariant variant) throws WriterException, IOException {
        String barcodeText = variant.getBarcode();
        System.out.println("DEBUG: generateAndSaveProductVariantBarcode called for variant: " + variant.getSku()
                + ", Barcode: " + barcodeText);

        if (barcodeText == null || barcodeText.isEmpty()) {
            throw new IllegalArgumentException("Barcode text is required to generate barcode image.");
        }

        byte[] barcodeImage = generateBarcodeImage(barcodeText, QR_CODE_WIDTH, 150); // Adjusted height for barcode

        // Format: variant name + millisec + barcode.jpeg
        String variantName = variant.getProduct().getName().replaceAll("\\s+", "_"); // Replace spaces for safety
        String fileName = variantName + System.currentTimeMillis() + barcodeText + ".jpeg";
        System.out.println("DEBUG: Generated filename: " + fileName);

        String path = fileStorageService.storeFileWithCustomName(barcodeImage, fileName, "barcodes");
        System.out.println("DEBUG: Stored barcode at path: " + path);
        return path;
    }
}
