/**
 * Copyright (C) 2012
 * by 52 North Initiative for Geospatial Open Source Software GmbH
 *
 * Contact: Andreas Wytzisk
 * 52 North Initiative for Geospatial Open Source Software GmbH
 * Martin-Luther-King-Weg 24
 * 48155 Muenster, Germany
 * info@52north.org
 *
 * This program is free software; you can redistribute and/or modify it under
 * the terms of the GNU General Public License version 2 as published by the
 * Free Software Foundation.
 *
 * This program is distributed WITHOUT ANY WARRANTY; even without the implied
 * WARRANTY OF MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program (see gnu-gpl v2.txt). If not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA or
 * visit the Free Software Foundation web page, http://www.fsf.org.
 */
/**
 * 
 */
package org.n52.ses.services.enrichment.aixm;

import java.util.ArrayList;
import java.util.List;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.n52.ses.api.exception.GMLParseException;

import aero.aixm.schema.x51.RunwayDocument;
import aero.aixm.schema.x51.RunwayType;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;

/**
 * Parser of features of an aixm runway. 
 * Creates the bounding box of all its geometries.
 * 
 * @author Klaus Drerup <klaus.drerup@uni-muenster.de>
 *
 */
public class RunwayParser extends AbstractGeometryParser {

	/* (non-Javadoc)
	 * @see org.n52.ses.enrichment.parser.AIXMAbstractGeometryParser#parseGeometries(org.apache.xmlbeans.XmlObject)
	 */
	@Override
	protected List<Geometry> parseGeometries(XmlObject xmlO)
			throws XmlException, ParseException, GMLParseException {
		List<Geometry> geometries = new ArrayList<Geometry>();
		
		// parse the feature's xml-document
		RunwayDocument runwayDoc = RunwayDocument.Factory.parse(xmlO.xmlText());
		RunwayType runway = runwayDoc.getRunway();
		
		// parse bounded by
		if (geometries.size() == 0 && runway.isSetBoundedBy()){
			geometries.add(super.parseBoundedBy(runway.getBoundedBy()));
		}
		
		return null;
	}

}
