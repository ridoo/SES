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
package org.n52.ses.util.postgres;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.n52.oxf.conversion.unit.NumberWithUOM;
import org.n52.ses.api.IUnitConverter;
import org.n52.ses.util.geometry.GeodesicApproximationTools;
import org.n52.ses.util.geometry.ICreateBuffer;
import org.n52.ses.util.postgres.PostgresConnection;
import org.n52.ses.util.unitconversion.SESUnitConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

/**
 * 
 * @author Matthes Rieke
 *
 */
public class PostGisBuffer implements ICreateBuffer {
	
	private static final Logger logger = LoggerFactory
			.getLogger(PostGisBuffer.class);
	
	private Connection connection;
	private IUnitConverter converter;

	/**
	 * 
	 * Constructor
	 *
	 */
	public PostGisBuffer() {
		this.converter = new SESUnitConverter();

		try {
			initDBConnection();
		} catch (SQLException e) {
			PostGisBuffer.logger.warn(e.getMessage(), e);
		}
	}


	public Geometry buffer(Geometry geom, double distance, String ucumUom,
			String crs) {

		NumberWithUOM values = this.converter.convert(ucumUom, distance);

		if (!values.getUom().equals("m")) {
			throw new IllegalStateException("Could not convert uom: "+ucumUom);
		}

		List<String> resultSet = null;

		try {
			resultSet = invokeQuery(createBufferStatement(geom, values.getValue() ));
		} catch (NumberFormatException e1) {
			logger.warn(e1.getMessage(), e1);
		} catch (SQLException e1) {
			logger.warn( e1.getMessage(), e1);
		}

		String wktString;
		if (resultSet != null) {
			wktString = resultSet.get(0);
		} else {
			throw new IllegalStateException("Could not receive result from PostGIS.");
		}

		try {
			return new WKTReader().read(wktString);
		} catch (ParseException e) {
			throw new IllegalStateException(e);
		}
	}


	private void initDBConnection() throws SQLException {
		this.connection = PostgresConnection.getInstance().getConnection();
		PostGisBuffer.logger.info("... connection initalized");
	}

	private List<String> invokeQuery(String query) throws SQLException {
		PostGisBuffer.logger.info("invoking postgis query:\n\t" + query);
		if (this.connection != null) {

			ArrayList<String> result = new ArrayList<String>();
			Statement st = this.connection.createStatement();
			ResultSet rs = st.executeQuery(query);

			StringBuilder log = new StringBuilder();
			log.append("query result:");
			while (rs.next()) {
				result.add(rs.getString(1));
				log.append("\n\t" + rs.getString(1));
			}
			PostGisBuffer.logger.info(log.toString());

			rs.close();
			st.close();
			return result;
		}
		
		PostGisBuffer.logger.warn("connection is null");
		return null;
	}

	private static String createBufferStatement(Geometry geom, double distMeter) {
		List<Geometry> geomList = resolveSegmentizedGeometry(geom);
		
		StringBuilder sb = new StringBuilder();
		sb.append("Select st_astext(");
		if (geomList.size() == 1) {
			sb.append("geometry(st_buffer(geography(st_geomfromtext('");
			sb.append(geomList.get(0).toText());
			sb.append("', 4326)), ");
			sb.append(distMeter);
			sb.append("))");
		} else if (geomList.size() > 1){
			createBufferUnionStatement(geomList, sb, distMeter);
		} else {
			throw new IllegalStateException("No geometry available!");
		}
		sb.append(")"); // end st_astext

		return sb.toString();
	}
	
	private static void createBufferUnionStatement(List<Geometry> geomList,
			StringBuilder sb, double distMeter) {
		sb.append("st_union(");
		sb.append("ARRAY[");
		for (Geometry geometry : geomList) {
			sb.append("geometry(st_buffer(geography(st_geomfromtext('");
			sb.append(geometry.toText());
			sb.append("', 4326)), ");
			sb.append(distMeter);
			sb.append("))");
			sb.append(",");
		}
		sb.delete(sb.length()-1, sb.length());
		sb.append("]"); // end ARRAY
		sb.append(")"); // end st_union
	}


	private static List<Geometry> resolveSegmentizedGeometry(Geometry geom) {
		List<Geometry> result = new ArrayList<Geometry>();
		
		if (geom instanceof Point) {
			result.add(geom);
		} else if (geom instanceof LineString) {
			resolveLineStringSegments((LineString) geom, result);
		} else {
			throw new UnsupportedOperationException("Only Point and LineString are currently supported for Buffering.");
		}
		
		return result;
	}


	private static void resolveLineStringSegments(LineString geom, List<Geometry> result) {
		for (int i = 0; i < geom.getCoordinateSequence().size() - 1; i++) {
			result.add(geom.getFactory().createLineString(new Coordinate[] {
					geom.getCoordinateSequence().getCoordinate(i),
					geom.getCoordinateSequence().getCoordinate(i+1)
			}));
		}
	}
	
	
	public static void main(String[] args) throws ParseException {
		System.out.println(createBufferStatement(GeodesicApproximationTools.approximateGreatCircle(20, new Coordinate(-87.90381968189912,41.97626011616167), new Coordinate(24.8242444289068, 59.41329527536156)), 200000));
	}



}
