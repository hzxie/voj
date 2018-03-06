/* Verwandlung Online Judge - A cross-platform judge online system
 * Copyright (C) 2018 Haozhe Xie <cshzxie@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *                              _ooOoo_  
 *                             o8888888o  
 *                             88" . "88  
 *                             (| -_- |)  
 *                             O\  =  /O  
 *                          ____/`---'\____  
 *                        .'  \\|     |//  `.  
 *                       /  \\|||  :  |||//  \  
 *                      /  _||||| -:- |||||-  \  
 *                      |   | \\\  -  /// |   |  
 *                      | \_|  ''\---/''  |   |  
 *                      \  .-\__  `-`  ___/-. /  
 *                    ___`. .'  /--.--\  `. . __  
 *                 ."" '<  `.___\_<|>_/___.'  >'"".  
 *                | | :  `- \`.;`\ _ /`;.`/ - ` : | |  
 *                \  \ `-.   \_ __\ /__ _/   .-` /  /  
 *           ======`-.____`-.___\_____/___.-`____.-'======  
 *                              `=---=' 
 *
 *                          HERE BE BUDDHA
 *
 */
package org.verwandlung.voj.judger.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Contains helper methods for loading native libraries, particularly JNI.
 * 
 * @author gkubisa
 */
public class NativeLibraryLoader {
	/**
	 * Utility classes should not have a public constructor.
	 */
	private NativeLibraryLoader() { }
	
	/**
	 * Loads a native shared library. It tries the standard System.loadLibrary
	 * method first and if it fails, it looks for the library in the current
	 * class path. It will handle libraries packed within jar files, too.
	 *
	 * @param libraryName - name of the library to load
	 * @throws IOException if the library cannot be extracted from a jar file
	 * into a temporary file
	 */
	public static void loadLibrary(String libraryName) throws IOException {
		try {
			System.loadLibrary(libraryName);
		} catch (UnsatisfiedLinkError e) {
			String fileName = System.mapLibraryName(libraryName);

			int dotPosition = fileName.lastIndexOf('.');
			File file = File.createTempFile(fileName.substring(0, dotPosition), fileName.substring(dotPosition));
			file.deleteOnExit();
			
			byte[] buffer = new byte[4096];
			InputStream inputStream = NativeLibraryLoader.class.getClassLoader().getResourceAsStream(fileName);
			OutputStream outputStream = new FileOutputStream(file);
			
			try {
				while ( inputStream.available() > 0 ) {
					int StreamLength = inputStream.read(buffer);
					if ( StreamLength >= 0 ) {
						outputStream.write(buffer, 0, StreamLength);
					}
				}
			} finally {
				outputStream.close();
				inputStream.close();
			}
			
			System.load(file.getAbsolutePath());
		}
	}
}
