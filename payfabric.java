public class PayFabric
{
	private static final String DEVICEID = '0052355f-d72a-e4cc-bd0a-c61b9376c945';
	private static final String DEVICEPW = 'qwer1234';
	private static final String DEVICEIDPW = DEVICEID + '|' + DEVICEPW;
	private static final String URLCREATE = 'https://sandbox.payfabric.com/v2/rest/api/transaction/create';
	private static final String URLPROCESS = 'https://sandbox.payfabric.com/v2/rest/api/transaction/process/{0}?cvc={1}';
	private static final String PAYMENTGATEWAY = 'Authorize Net Test';
	
	private final sObject sObj;
	private String key;
	private String status;
	private String message;
	private String errors = '';
	
	public PayFabric(ApexPages.StandardController stdController)
	{
		stdController.addFields(new List<String> {'Status'});
		sObj = stdController.getRecord();
	}
	
	public void init()
	{
		Map<String, String> params = ApexPages.currentPage().getParameters();
		String creationResponse = createTransaction
		(
			params.get('oi_total'),
			params.get('cc_number'),
			formatExpDate(params.get('cc_month'), params.get('cc_year')),
			params.get('cc_firstname'),
			params.get('cc_lastname')
		);
		Map<String, String> json1 = parseJson(creationResponse);
		if (json1 != null)
		{
			key = json1.get('Key');
			String processResponse = processTransaction(key, params.get('cc_cvv2'));
			Map<String, String> json2 = parseJson(processResponse);
			if (json2 != null)
			{
				status = json2.get('Status');
				message = json2.get('Message');
				if (status.equals('Approved'))
				{
					sObj.put('Status', 'Activated');
					update sObj;
				}
			}
		}
	}
	
	public String getKey()
	{
		return key;
	}
	
	public String getMessage()
	{
		return message;
	}
	
	public String getStatus()
	{
		return status;
	}
	
	public String getErrors()
	{
		return errors;
	}
	
	private String createTransaction(String price, String ccNum, String expDate, String firstName, String lastName)
	{
		String data = '{"SetupId":"'+PAYMENTGATEWAY+'","Type":"Sale","Customer":"PayFabric","Amount":'+price+',"Currency":"USD","Card":{"Account":"'+ccNum+'","ExpDate":"'+expDate+'","CardHolder":{"FirstName":"'+firstName+'","LastName":"'+lastName+'"}}}';
		Http h = new Http();
		HttpRequest req = new HttpRequest();
		req.setEndpoint(URLCREATE);
		req.setMethod('POST');
		req.setHeader('authorization', DEVICEIDPW);
		req.setHeader('content-type', 'application/json');
		req.setBody(data);
		HttpResponse res = h.send(req);
		return res.getBody();
	}
	
	private String processTransaction(String key, String cvc)
	{
		Http h = new Http();
		HttpRequest req = new HttpRequest();
		req.setEndpoint(String.format(URLPROCESS, new String[] {key, cvc}));
		req.setMethod('GET');
		req.setHeader('authorization', DEVICEIDPW);
		HttpResponse res = h.send(req);
		return res.getBody();
	}
	
	private String formatExpDate(String month, String year)
	{
		if (month == null || year == null) {return null;}
		month = month.deleteWhitespace();
		year = year.deleteWhitespace();
		month = '0' + month;
		year = '0' + year;
		month = month.right(2);
		year = year.right(2);
		return month + year;
	}
	
	private Map<String, String> parseJson(String JSONContent)
	{
		Map<String, String> temp = new Map<String, String>();
		try
		{
			JSONParser parser = JSON.createParser(JSONContent);
			while (parser.nextToken() != null && parser.nextValue() != null)
			{
				temp.put(parser.getCurrentName(), parser.getText());
			}
		}
		catch (JSONException ex)
		{
			errors += JSONContent;
			return null;
		}
		return temp;
	}
}