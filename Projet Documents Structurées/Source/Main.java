import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class Main {
	static ArrayList<String> list = new ArrayList<String>();
	static Document ResultDocument;
	static Element MainElement;
	static DocumentBuilder builder;

	public static void HandleFiles(String Path) throws Exception {
		Path = Path.replaceAll("\\\\$", "");
		File fl = new File(Path);
		if (fl.isDirectory()) {
			for (String f : fl.list())
				if (!f.contentEquals(".") && !f.contentEquals(".."))
					HandleFiles(Path + "\\" + f);
		} else if (Path.contains("poeme.txt"))
			neruda(Path);
		else if (Path.contains("boitedialog.fxml"))
			TransformJavaFX(Path);
		else if (Path.contains("renault.html"))
			TransformRenault(Path);
		else if (Path.contains("M457.xml"))
			TransformMxxx(Path);
		else if (Path.contains("M674.xml"))
			TransformMxxx(Path);
		else if (Path.contains("fiches.txt")) {
			fiche(Path, true);
			fiche(Path, false);
		}
	}

	public static void main(String[] args) throws Exception {
		if (args.length == 0) {
			JOptionPane.showMessageDialog(new JPanel(),
					"Attention vous avez oublié de spécifier le nom du répertoire à traiter !", "Error",
					JOptionPane.ERROR_MESSAGE);
			return;
		}
		if (args.length != 1) {
			JOptionPane.showMessageDialog(new JPanel(), "Attention le répertoire ne doit pas contenir des espaces !",
					"Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		args[0] = args[0].replaceAll("\"", "");
		args[0] = args[0].replaceAll("\\\\$", "");
		(new File("Sortie")).mkdirs();
		Process p = Runtime.getRuntime().exec("python Preprocess.py " + '"' + args[0] + '"');
		while (p.isAlive())
			;
		builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		builder.setEntityResolver(new EntityResolver() {
			@Override
			public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
				if (systemId.contains(".dtd")) {
					return new InputSource(new StringReader(""));
				} else {
					return null;
				}
			}
		});
		HandleFiles(args[0]);
		System.out.println("Done\n");
	}

	public static void Convert(Node SourceNode, String s) {
		Element ResultElement = null;
		if (s.equals("p") && SourceNode.getNodeValue() != null && !SourceNode.getNodeValue().matches("\\s+")) {
			ResultElement = ResultDocument.createElement("texte");
			ResultElement.appendChild(
					ResultDocument.createTextNode(SourceNode.getNodeValue().replace("\n", "").replace("\t", "")));
			MainElement.appendChild(ResultElement);
		}
		for (int i = 0; i < SourceNode.getChildNodes().getLength(); i++) {
			Convert(SourceNode.getChildNodes().item(i), SourceNode.getNodeName());
		}
	}

	public static void Renault(Node SourceNode, String s, boolean InsideP) {
		if (SourceNode.getNodeName().equals("p"))
			InsideP = true;
		if (!InsideP && list.size() > 2 && list.get(1).equals("Adresse")) {
			Element Nom = ResultDocument.createElement("Nom");
			Nom.appendChild(ResultDocument.createTextNode(list.get(0)));
			MainElement.appendChild(Nom);
			Nom = ResultDocument.createElement("Adresse");
			Nom.appendChild(ResultDocument.createTextNode(list.get(2).replace("\n", "").replace(" ", "").equals(":")
					? list.get(3).replace("\n", " ").replaceAll("^\\s*", "")
					: list.get(2).replace("\n", " ").replace(":", "").replaceAll("^\\s*", "")));
			MainElement.appendChild(Nom);
			int tel = list.indexOf("Tél") + 1;
			Nom = ResultDocument.createElement("Num_téléphone");
			Nom.appendChild(ResultDocument.createTextNode(list.get(tel).replace("\n", "").replace(" ", "").equals(":")
					? list.get(tel + 1).replace("\n", " ").replaceAll("^\\s*", "")
					: list.get(tel).replace("\n", " ").replace(":", "").replaceAll("^\\s*", "")));
			MainElement.appendChild(Nom);
		}
		if (!InsideP)
			list = new ArrayList<String>();
		if (InsideP && SourceNode.getNodeValue() != null && !SourceNode.getNodeValue().matches("(\\s*\\n*\u00A0*)*"))
			list.add(SourceNode.getNodeValue());
		for (int i = 0; i < SourceNode.getChildNodes().getLength(); i++) {
			Renault(SourceNode.getChildNodes().item(i), SourceNode.getNodeName(), InsideP);
		}
	}

	public static void JavaFX(Node SourceNode) {
		if (SourceNode.hasAttributes())
			for (int i = 0; i < SourceNode.getAttributes().getLength(); i++) {
				Element e = ResultDocument.createElement("texte");
				e.setAttribute(SourceNode.getAttributes().item(i).getNodeName(), "x");
				e.appendChild(ResultDocument.createTextNode(SourceNode.getAttributes().item(i).getNodeValue()));
				MainElement.appendChild(e);
			}
		for (int i = 0; i < SourceNode.getChildNodes().getLength(); i++) {
			JavaFX(SourceNode.getChildNodes().item(i));
		}
	}

	public static void CreateXML(String outputPath, Document document, String dtd, boolean PrettyPrint)
			throws Exception {
		document.setXmlStandalone(true);
		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		if (PrettyPrint)
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "20");
		if (dtd != null) {
			DOMImplementation domImpl = document.getImplementation();
			DocumentType doctype = domImpl.createDocumentType("doctype", null, dtd);
			transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, doctype.getSystemId());
		}
		DOMSource domSource = new DOMSource(document);
		transformer.transform(domSource, new StreamResult(new File("Sortie\\" + outputPath)));
		byte[] encoded = Files.readAllBytes(Paths.get("Sortie\\" + outputPath));
		String pp = new String(encoded, StandardCharsets.UTF_8);
		pp = pp.replace("?><", "?>\r\n<");
		if (PrettyPrint)
			pp = '\uFEFF' + pp.replaceAll(" {20}", "\t");
		if (outputPath.contains("javafx.xml"))
			pp = pp.substring(1, pp.length() - 2);
		if (outputPath.contains("renault.xml"))
			pp = '\uFEFF' + pp;
		Files.delete(Paths.get("Sortie\\" + outputPath));
		FileOutputStream fos = new FileOutputStream("Sortie\\" + outputPath);
		fos.write(pp.getBytes(StandardCharsets.UTF_8));
		fos.close();
	}

	public static void TransformMxxx(String path) throws Exception {
		String outputName = null;
		if (path.contains("M674.xml"))
			outputName = "sortie1.xml";
		else if (path.contains("M457.xml"))
			outputName = "sortie2.xml";
		Document SourceDocument = builder.parse(new File(path));
		ResultDocument = DocumentBuilderFactory.newInstance().newDocumentBuilder().getDOMImplementation().createDocument(null,"TEI_S", null);
		MainElement = ResultDocument.getDocumentElement();
		MainElement = (Element) MainElement.appendChild(ResultDocument.createElement(path.substring(path.lastIndexOf('\\') + 1)));
		Convert(SourceDocument.getDocumentElement(), "Source Element");
		CreateXML(outputName, ResultDocument, "dom.dtd", false);
	}

	public static void TransformRenault(String path) throws Exception {
		Document SourceDocument = builder.parse(new File(path));
		ResultDocument = DocumentBuilderFactory.newInstance().newDocumentBuilder().getDOMImplementation().createDocument(null,"Concessionnaires", null);
		MainElement = ResultDocument.getDocumentElement();
		Renault(SourceDocument.getDocumentElement(), "Source Element", false);
		CreateXML("renault.xml", ResultDocument, null, false);
	}

	public static void TransformJavaFX(String path) throws Exception {
		Document SourceDocument = builder.parse(new File(path));
		ResultDocument = DocumentBuilderFactory.newInstance().newDocumentBuilder().getDOMImplementation().createDocument(null,"Racine", null);
		MainElement = ResultDocument.getDocumentElement();
		MainElement.setAttribute("xmlns:fx", "http://javafx.com/fxml");
		JavaFX(SourceDocument.getDocumentElement());
		CreateXML("javafx.xml", ResultDocument, null, true);
	}

	public static void neruda(String path) throws Exception {
		ResultDocument = DocumentBuilderFactory.newInstance().newDocumentBuilder().getDOMImplementation().createDocument(null,"poema", null);
		MainElement = ResultDocument.getDocumentElement();
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		String poemo = new String(encoded, StandardCharsets.UTF_8);
		Matcher matcher = Pattern.compile("(.*?\\n)*?(?=\\n)").matcher(poemo);
		Element titulo = ResultDocument.createElement("titulo");
		matcher.find();
		titulo.appendChild(ResultDocument.createTextNode(matcher.group(0).replace("\n", "")));
		MainElement.appendChild(titulo);
		while (matcher.find()) {
			if (!matcher.group(0).isEmpty()) {
				Element estrofa = ResultDocument.createElement("estrofa");
				Matcher Versomatcher = Pattern.compile(".*").matcher(matcher.group(0));
				while (Versomatcher.find()) {
					if (!Versomatcher.group(0).isEmpty()) {
						Element verso = ResultDocument.createElement("verso");
						verso.appendChild(ResultDocument.createTextNode(Versomatcher.group(0)));
						estrofa.appendChild(verso);
					}
				}
				MainElement.appendChild(estrofa);
			}
		}
		CreateXML("neruda.xml", ResultDocument, "neruda.dtd", false);
	}

	public static void fiche(String path, boolean isF1) throws Exception {
		ArrayList<Element> list = new ArrayList<Element>();
		ResultDocument = DocumentBuilderFactory.newInstance().newDocumentBuilder().getDOMImplementation().createDocument(null,"FICHES", null);
		MainElement = ResultDocument.getDocumentElement();
		Node xsl = ResultDocument.createProcessingInstruction("xml-stylesheet", "type=\"text/xsl\" href=\"fiche.xsl\"");
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		String fiches = new String(encoded, StandardCharsets.UTF_8);
		Matcher matcher = Pattern.compile("((.+?\r\n)*)(?=\\r\\n\\r\\n)").matcher(fiches);
		int i = 0;
		while (matcher.find()) {
			if (!matcher.group(1).matches("[\\n\\r\\s]*")) {
				list.clear();
				Element FICHE = ResultDocument.createElement("FICHE");
				FICHE.setAttribute("id", Integer.toString(++i));
				Matcher smatcher = Pattern
						.compile("(.*?[\\t: ]*)(((?<!\\w)[A-Z]{2}(?!(\\w| \\w| \\w\\w\\w))[\\t: ]*)+)")
						.matcher(matcher.group(1));
				Element langue = null;
				boolean isRF = false;
				while (smatcher.find()) {
					if (!smatcher.group(2).isEmpty()) {
						String g2 = smatcher.group(2).replaceAll("[\\s\\t:]", "");
						if (smatcher.group(1).replace("\n", "").equals("")) {
							if (langue != null)
								FICHE.appendChild(langue);
							langue = ResultDocument.createElement("Langue");
							langue.setAttribute("id", g2);
							for (Element e : list)
								langue.appendChild(e.cloneNode(true));
							isRF = false;
						} else {
							String tmp = g2, tmp2 = smatcher.group(2), content = "", head;
							tmp2 = tmp2.replace(" :", "");
							if (tmp.contains("RF")) {
								isRF = true;
								tmp = tmp.replace("RF", "");
								tmp2 = tmp2.replace("RF", "");
							}
							while (!tmp.isEmpty()) {
								content = tmp.substring(0, 2) + " : " + content;
								tmp2 = tmp2.replace(tmp.substring(0, 2), "");
								tmp = tmp.length() > 2 ? tmp.substring(2) : "";
							}
							if (isRF) {
								content = "RF | " + content;
							}
							head = content.substring(0, 2);
							Element line = ResultDocument.createElement(head);
							content = content.replace("BE : ", "");
							line.appendChild(ResultDocument.createTextNode(content + smatcher.group(1) + tmp2));
							if (langue == null && isF1) {
								if (head.equals("BE") || head.equals("TY") || head.equals("AU"))
									FICHE.appendChild(line);
								else
									list.add(line);
							} else if (langue == null)
								FICHE.appendChild(line);
							else
								langue.appendChild(line);
						}
					}
				}
				if (langue != null)
					FICHE.appendChild(langue);
				MainElement.appendChild(FICHE);
			}
		}
		ResultDocument.insertBefore(xsl, MainElement);
		CreateXML("fiches" + (isF1 ? "1" : "2") + ".xml", ResultDocument, isF1 ? "fiches.dtd" : null, true);
	}
}
