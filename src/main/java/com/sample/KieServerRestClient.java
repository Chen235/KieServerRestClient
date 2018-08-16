package com.sample;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.drools.core.command.runtime.BatchExecutionCommandImpl;
import org.drools.core.command.runtime.rule.FireAllRulesCommand;
import org.drools.core.command.runtime.rule.InsertObjectCommand;
import org.kie.api.command.Command;
import org.kie.api.runtime.ExecutionResults;
import org.kie.server.api.marshalling.MarshallingFormat;
import org.kie.server.api.model.KieContainerResource;
import org.kie.server.api.model.KieContainerResourceList;
import org.kie.server.api.model.KieServerInfo;
import org.kie.server.api.model.KieServiceResponse.ResponseType;
import org.kie.server.api.model.ServiceResponse;
import org.kie.server.client.KieServicesClient;
import org.kie.server.client.KieServicesConfiguration;
import org.kie.server.client.KieServicesFactory;
import org.kie.server.client.RuleServicesClient;

import com.redhat.demos.dm.loan.model.Applicant;
import com.redhat.demos.dm.loan.model.Loan;

public class KieServerRestClient {

	public static void main(String[] args) {
	    String url = "http://localhost:8080/kie-server/services/rest/server";
	    String user = "dmAdmin";
	    String password = "redhatdm1!";

	    String containerId = "loan-application_1.1.0";
	    String sessionId = "default-stateless-ksession";

	    MarshallingFormat format = MarshallingFormat.JSON;

	    Map<String, Object> facts = new HashMap<String, Object>();

		KieServicesClient ksc = init(url, user, password, format);
		//listContainers(ksc);
		//listCapabilities(ksc);
		
		
		Applicant applicant = new Applicant();
		applicant.setCreditScore(230);
		applicant.setName("Jim-Whitehurst");

    	facts.put(applicant.getName(), applicant);
    	
    	Loan loan = new Loan();
    	loan.setAmount(2500);
    	loan.setApproved(false);
    	loan.setDuration(24);
    	loan.setInterestRate(1.5);
    	
    	facts.put("loan", loan);
    	
		try {
			ExecutionResults results = executeCommands(ksc, containerId, sessionId, facts);
			if (results != null) {
				Collection<String> ids = results.getIdentifiers();
				ids.forEach(id -> {
					if (results.getValue(id).getClass().getName().equals("com.redhat.demos.dm.loan.model.Applicant")) {
						Applicant apc = (Applicant) results.getValue(id);
						System.out.println("applicant : Applicant [name=" + id + ", creditScore=" + apc.getCreditScore() + "]");
					} else				
						System.out.println(id + " : " + results.getValue(id));
				});
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
    public static KieServicesClient init(String url, String user, String password, MarshallingFormat format) {
    	KieServicesConfiguration conf = KieServicesFactory.newRestConfiguration(url, user, password);
    	conf.setMarshallingFormat(format);
        return KieServicesFactory.newKieServicesClient(conf); 
    }
    
    
	public static ExecutionResults executeCommands(KieServicesClient kieServicesClient, String ContainerId, String SessionId, Map<String, Object> facts) throws Exception {
		 //System.out.println("== Sending commands to the server ==");
		 RuleServicesClient rulesClient = kieServicesClient.getServicesClient(RuleServicesClient.class);

		 List<Command<?>> commands = new ArrayList<Command<?>>();

		 facts.forEach((id,fact) -> {
			 commands.add(new InsertObjectCommand(fact, id));
		 });
		 
		 FireAllRulesCommand fireAllRulesCmd = new FireAllRulesCommand();
		 commands.add(fireAllRulesCmd);

		 BatchExecutionCommandImpl executionCommand = new BatchExecutionCommandImpl(commands);
		 executionCommand.setLookup(SessionId);

		 ServiceResponse<ExecutionResults> executeResponse = rulesClient.executeCommandsWithResults(ContainerId,
					executionCommand);
		 
		 if(executeResponse.getType() == ResponseType.SUCCESS) {
			//System.out.println("Commands executed with success! Response: ");
			return executeResponse.getResult();
		 } else {
			System.out.println("Error executing rules. Message: ");
			System.out.println(executeResponse.getMsg());
			return null;
		}		
	}
	
	public static void listContainers(KieServicesClient kieServicesClient) {
		  KieContainerResourceList containersList = kieServicesClient.listContainers().getResult();
		  List<KieContainerResource> kieContainers = containersList.getContainers();
		  System.out.println("Available containers: ");
		  for (KieContainerResource container : kieContainers) {
		      System.out.println("\t" + container.getContainerId() + " (" + container.getReleaseId() + ")");
		  }
	}
	
	public static void listCapabilities(KieServicesClient kieServicesClient) {
	    KieServerInfo serverInfo = kieServicesClient.getServerInfo().getResult();
	    System.out.print("Server capabilities:");
	    for(String capability: serverInfo.getCapabilities()) {
	        System.out.print(" " + capability);
	    }
	    System.out.println();
	}
 }
