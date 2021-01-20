package de.siphalor.nmuk;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class NMUK {

	public static Logger LOGGER = LogManager.getLogger();

	public static final String MOD_ID = "nmuk";
	public static final String MOD_NAME = "No More Useless Keys";

	public static void log(Level level, String message) {
		LOGGER.log(level, "[" + MOD_NAME + "] " + message);
	}

}
