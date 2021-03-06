/*
 * Copyright (C) 2004 Rong Chen, Acode HB, Sweden
 * All rights reserved.
 *
 * The contents of this software are subject to the FSF GNU Public License 2.0;
 * you may not use this software except in compliance with the License. You may
 * obtain a copy of the License at http://www.fsf.org/licenses/gpl.html
 *
 * Software distributed under the License is distributed on an 'AS IS' basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 */
package org.openehr.rm.demographic;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.hibernate.Session;
import org.junit.Test;

/**
 * EventContextTest
 *
 * @author Rong Chen
 * @version 1.0
 */
public class AddressHibernateTest extends DemographicHibernateTestBase {

	@Test
	public void testSaveLoad() throws Exception {	

		Address ad = new Address(oid("address.trial"), "at0000",
                text("address meaning"), null, null, null, null,
                itemSingle("address details"));

		Session session = sessionFactory.getCurrentSession();
		session.save(ad);
		Address a1 = (Address)session.load(Address.class, ad.getMappingId());
		assertThat(a1, is(ad));

    }

}
