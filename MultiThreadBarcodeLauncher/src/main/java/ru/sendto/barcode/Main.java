package ru.sendto.barcode;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.imageio.ImageIO;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Reader;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.GlobalHistogramBinarizer;
import com.google.zxing.multi.GenericMultipleBarcodeReader;
import com.google.zxing.multi.MultipleBarcodeReader;

/**
 * 
 * @author Lev Nadeinsky
 * @date	2016-11-27
 */
public class Main {
	private static final Map<DecodeHintType, Object> HINTS;

	static {
		HINTS = new EnumMap<>(DecodeHintType.class);
		HINTS.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
		HINTS.put(DecodeHintType.POSSIBLE_FORMATS, EnumSet.allOf(BarcodeFormat.class));
	}

	public static void main(String[] args) throws Exception {

		if (args == null || args.length == 0) {
			System.err.println("there is no img as arg.");
			return;
		}

		scan(args);

		// CommandLineRunner.main(args);
	}

	private static void scan(String[] args) throws InterruptedException {
		ExecutorService es = Executors.newWorkStealingPool(Runtime.getRuntime().availableProcessors() * 2);
		Future<Result[]>[] res = new Future[args.length];
		
		for (int i =0; i<args.length;i++) {
			
			BufferedImage image;
			String tmp = args[i];
			try {
				image = ImageIO.read(new File(tmp));

				res[i] = es.submit(() -> scanImage(tmp, image));

			} catch (Exception e) {
				System.err.println(tmp + " error:");
				e.printStackTrace();
			}

		}
		for (int i =0; i<res.length;i++) {
			Future<Result[]> f = res[i];
			if(f==null)
				continue;
			try {
				printResults(args[i], f.get());
			} catch (ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	private static Result[] scanImage(String file, BufferedImage image) {
		LuminanceSource source = new BufferedImageLuminanceSource(image);
		BinaryBitmap bitmap = new BinaryBitmap(new GlobalHistogramBinarizer(source));

		Reader reader = new MultiFormatReader();

		MultipleBarcodeReader multiReader = new GenericMultipleBarcodeReader(reader);
		Result[] theResults;
		try {
			theResults = multiReader.decodeMultiple(bitmap, HINTS);
			return theResults;
			//printResults(file, theResults);
		} catch (NotFoundException e) {
			System.err.println(file + " error:");
			e.printStackTrace();
		}
		return null;
	}

	private static void printResults(String file, Result[] theResults) {
		if (theResults != null) {
			for (Result result : theResults) {
				System.out.format("%s;%s;%s;", file, result.getText(), result.getBarcodeFormat().name());
				for (ResultPoint point : result.getResultPoints()) {
					System.out.format("(%.0f,%.0f)", point.getX(), point.getY());
				}
				System.out.println();
			}
		}
	}

}
