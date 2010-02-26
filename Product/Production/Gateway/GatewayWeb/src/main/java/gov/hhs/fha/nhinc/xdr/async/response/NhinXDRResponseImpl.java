/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package gov.hhs.fha.nhinc.xdr.async.response;
import gov.hhs.fha.nhinc.adapter.xdr.async.response.proxy.AdapterXDRResponseProxy;
import gov.hhs.fha.nhinc.adapter.xdr.async.response.proxy.AdapterXDRResponseProxyObjectFactory;
import gov.hhs.fha.nhinc.common.nhinccommon.AcknowledgementType;
import gov.hhs.fha.nhinc.common.nhinccommon.AssertionType;
import gov.hhs.fha.nhinc.nhinclib.NhincConstants;
import gov.hhs.fha.nhinc.properties.PropertyAccessException;
import gov.hhs.fha.nhinc.properties.PropertyAccessor;
import gov.hhs.fha.nhinc.saml.extraction.SamlTokenExtractor;
import javax.xml.ws.WebServiceContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import gov.hhs.fha.nhinc.xdr.XDRAuditLogger;
import gov.hhs.fha.nhinc.xdr.XDRPolicyChecker;
import oasis.names.tc.ebxml_regrep.xsd.rs._3.RegistryResponseType;

/**
 *
 * @author patlollav
 */
public class NhinXDRResponseImpl
{
    public static final String XDR_RESPONSE_SUCCESS = "Success";
    public static final String XDR_RESPONSE_FAILURE = "Failure";
    public static final String XDR_POLICY_ERROR = "CONNECTPolicyCheckFailed ";
    
    private static final Log logger = LogFactory.getLog(NhinXDRResponseImpl.class);

    /**
     *
     * @return
     */
    protected Log getLogger(){
        return logger;
    }

    /**
     *
     * @param body
     * @param context
     * @return
     */
    public ihe.iti.xdr._2007.AcknowledgementType provideAndRegisterDocumentSetBResponse(RegistryResponseType body,WebServiceContext context ) {

        ihe.iti.xdr._2007.AcknowledgementType result;

        getLogger().debug("Entering provideAndRegisterDocumentSetBResponse");

        AssertionType assertion = createAssertion(context);

        AcknowledgementType ack = getXDRAuditLogger().auditNhinXDRResponse(body, assertion, NhincConstants.AUDIT_LOG_INBOUND_DIRECTION);

        getLogger().debug("Audit Log Ack Message:" + ack.getMessage());

        String localHCID = retrieveHomeCommunityID();

        getLogger().debug("Local Home Community ID: " + localHCID);

        if (isPolicyOk(body, assertion, assertion.getHomeCommunity().getHomeCommunityId(), localHCID))
        {
            getLogger().debug("Policy Check Succeeded");
            result = forwardToAgency(body, assertion);
        }
        else
        {
            getLogger().error("Policy Check Failed");
            result = createFailedPolicyCheckResponse();
        }

        //ack = auditLogger.auditNhinXDRResponse(result, assertion, NhincConstants.AUDIT_LOG_OUTBOUND_DIRECTION);

        logger.debug("Exiting provideAndRegisterDocumentSetBResponse");

         return result;
    }


        /**
     *
     * @param context
     * @return
     */
    protected AssertionType createAssertion(WebServiceContext context){
        AssertionType assertion = SamlTokenExtractor.GetAssertion(context);
        return assertion;
    }

    /**
     *
     * @return
     */
    protected String retrieveHomeCommunityID(){
        String localHCID = null;
        try {
            localHCID = PropertyAccessor.getProperty(NhincConstants.GATEWAY_PROPERTY_FILE, NhincConstants.HOME_COMMUNITY_ID_PROPERTY);
        } catch (PropertyAccessException ex) {
            logger.error("Exception while retrieving home community ID", ex);
        }

        return localHCID;
    }
    /**
     *
     * @return
     */
    protected XDRAuditLogger getXDRAuditLogger(){
        return new XDRAuditLogger();
    }

    /**
     *
     * @param body
     * @param context
     * @return
     */
    protected ihe.iti.xdr._2007.AcknowledgementType forwardToAgency(RegistryResponseType body, AssertionType assertion)
    {
        getLogger().debug("Entering forwardToAgency");

        AdapterXDRResponseProxyObjectFactory factory = new AdapterXDRResponseProxyObjectFactory();

        AdapterXDRResponseProxy proxy = factory.getAdapterXDRResponseProxy();

        ihe.iti.xdr._2007.AcknowledgementType response = proxy.provideAndRegisterDocumentSetBResponse(body, assertion);

        getLogger().debug("Exiting forwardToAgency");

        return response;
    }

    /**
     *
     * @param newRequest
     * @param assertion
     * @param senderHCID
     * @param receiverHCID
     * @return
     */
    protected boolean isPolicyOk(RegistryResponseType newRequest, AssertionType assertion, String senderHCID, String receiverHCID) {

        boolean bPolicyOk = false;

        logger.debug("checking the policy engine for the new request to a target community");

        //return true if 'permit' returned, false otherwise
        XDRPolicyChecker policyChecker = new XDRPolicyChecker();
        //return policyChecker.checkXDRRequestPolicy(newRequest, assertion,senderHCID ,receiverHCID);
        return true;

    }

    /**
     *
     * @return
     */
    private ihe.iti.xdr._2007.AcknowledgementType createFailedPolicyCheckResponse()
    {
        ihe.iti.xdr._2007.AcknowledgementType result= new ihe.iti.xdr._2007.AcknowledgementType();
        result.setMessage(XDR_POLICY_ERROR);
        return result;
    }

}
