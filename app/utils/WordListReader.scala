package utils

import java.io.{BufferedReader, InputStreamReader, FileInputStream}
import java.util.zip.GZIPInputStream
import play.api.Play
import play.api.Play.current
import utils.Loans.using

object WordListReader {

	val linePattern = """.*\[(.*)\].*""".r

	def getGzipReader(path: String, encoding: String) =
		new BufferedReader(
			 new InputStreamReader(
			 	new GZIPInputStream(
					new FileInputStream(Play.getFile(path))), encoding))
	
	
	def parse(line: String) = line match {
		case linePattern(word) => Some(word)
		case _ => None
	}

	def readWordsFromEdictGzip(path: String) = {
		def read(acc: List[String], reader: BufferedReader): List[String] = {
			val line = reader.readLine()
			if (line == null) acc
			else read(line :: acc, reader)
		}
		using (getGzipReader(path, "EUC-JP")) { reader =>
			read(Nil, reader).flatMap(parse).toSet
		}
	}
}