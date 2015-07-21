package nc.ui.wlm.m21.action;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import nc.ui.pu.m21.view.OrderBillForm;
import nc.ui.pu.uif2.PUBillManageModel;
import nc.ui.pub.beans.MessageDialog;
import nc.ui.pubapp.uif2app.query2.model.ModelDataManager;
import nc.ui.uif2.NCAction;
import nc.ui.wlm.ext.WLMExtUtil;
import nc.vo.pu.m21.entity.OrderVO;
import nc.vo.pub.BusinessException;
import nc.vo.pub.lang.UFBoolean;

public class ProcureRecallyAction extends NCAction {
	private final String BTNNAME = "�ٻ�";
	private final String ACTIONCODE = "ProcureRecallyAction";
	private ModelDataManager dataManager;
	private PUBillManageModel model;
	private OrderBillForm BillForm;
	private WLMExtUtil WLMExtUtils;
	
	public ProcureRecallyAction() {
		this.setCode(ACTIONCODE);
		this.setBtnName(BTNNAME);
		this.putValue(ACTIONCODE, ACTIONCODE);
	}
	
	public OrderBillForm getBillForm() {
		return BillForm;
	}

	public void setBillForm(OrderBillForm billForm) {
		BillForm = billForm;
	}

	public ModelDataManager getDataManager() {
		return dataManager;
	}

	public void setDataManager(ModelDataManager dataManager) {
		this.dataManager = dataManager;
	}

	public PUBillManageModel getModel() {
		return model;
	}

	public void setModel(PUBillManageModel model) {
		this.model = model;
	}

	

	@Override
	public void doAction(ActionEvent arg0) throws Exception {
		List<OrderVO> povolist = getOrderVO();
		if (povolist == null || povolist.size() <= 0) {
			MessageDialog.showHintDlg(getBillForm(), "�ڲ�Эͬ", "��ѡ����ٻصĵ���!");
			return;
		}
		for (int i = 0; i < povolist.size(); i++) {
			if(!povolist.get(i).getParentVO().getAttributeValue("forderstatus").toString().equals("3")){
				MessageDialog.showErrorDlg(getBillForm(), "�ڲ�Эͬ", "��ѡ���ݵĵ�"+(i+1)+"��û������ͨ����û���ٻ�!");
				return;
			}				
		if (povolist != null && povolist.size() > 0) {
			OrderVO[] povos = povolist.toArray(new OrderVO[0]);
			if (MessageDialog.showYesNoDlg(getBillForm(), "�ڲ�Эͬ", "�Ƿ��ٻص��ݺţ���" + povos[i].getParentVO().getAttributeValue("vbillcode").toString()+ "����Эͬ����?") == MessageDialog.ID_YES){
				try {
					UFBoolean ret =getWLMExtUtil().reCallCoorpBill(povos[i].getParentVO().getAttributeValue("pk_order").toString());
					if (ret.booleanValue()) {
						MessageDialog.showHintDlg(getBillForm(), "�ڲ�Эͬ",	"���ݺţ���" + povos[i].getParentVO().getAttributeValue("vbillcode").toString() + "����Эͬ�����ٻسɹ�!");
					}
				} catch (BusinessException e) {
					e.printStackTrace();
					MessageDialog.showErrorDlg(getBillForm(), "�ڲ�Эͬ", e.getMessage());
				}
			}
		}
		}
	}
	
	public WLMExtUtil getWLMExtUtil() throws BusinessException {
		if (this.WLMExtUtils == null) {
			this.WLMExtUtils = new WLMExtUtil();
		}
		return this.WLMExtUtils;	
	}
	
	private List<OrderVO> getOrderVO() throws BusinessException {
		Object[] objs =(Object[])getModel().getSelectedOperaDatas();
		List<OrderVO> OrderVO = new ArrayList<OrderVO>();
		if (objs!=null) {
			for (Object object : objs) {
				OrderVO.add((OrderVO)object);
			}		
		}
		return OrderVO;
	}
}
